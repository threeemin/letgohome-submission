package com.meraki.letgohome.route

import android.net.Uri
import com.meraki.letgohome.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL

internal data class GeoPoint(val latitude: Double, val longitude: Double) {
    init {
        require(latitude in -90.0..90.0 && longitude in -180.0..180.0)
    }

    fun asKakaoQuery() = "$longitude,$latitude"
}

internal data class DrivingRoute(
    val distanceMeters: Int,
    val durationSeconds: Int,
    val paths: List<List<GeoPoint>>,
    val isDemo: Boolean,
)

internal class RouteUnavailable : IOException("No driving route")

internal suspend fun loadDrivingRoute(
    origin: GeoPoint,
    destination: GeoPoint,
    demoOnly: Boolean,
): DrivingRoute = withContext(Dispatchers.IO) {
    val url = routeProxyEndpoint().buildUpon()
        .appendQueryParameter("origin", origin.asKakaoQuery())
        .appendQueryParameter("destination", destination.asKakaoQuery())
        .build()
    val (payload, isDemo) = readProxyResponse(url)
    if (demoOnly && !isDemo) throw IOException("Demo proxy expected")
    parseDrivingRoute(payload, isDemo)
}

internal data class DestinationPlace(val name: String, val address: String, val point: GeoPoint)

internal suspend fun searchDestinations(query: String, demoOnly: Boolean): List<DestinationPlace> =
    withContext(Dispatchers.IO) {
        val endpoint = routeProxyEndpoint()
        val url = endpoint.buildUpon()
            .path(endpoint.path.orEmpty().substringBeforeLast('/', "") + "/search")
            .appendQueryParameter("q", query)
            .build()
        val (payload, isDemo) = readProxyResponse(url)
        if (demoOnly && !isDemo) throw IOException("Demo proxy expected")
        val places = payload.getJSONArray("places")
        List(places.length()) { index ->
            val place = places.getJSONObject(index)
            DestinationPlace(
                name = place.getString("name"),
                address = place.getString("address"),
                point = GeoPoint(place.getDouble("lat"), place.getDouble("lon")),
            )
        }
    }

private fun routeProxyEndpoint(): Uri {
    val endpoint = Uri.parse(BuildConfig.ROUTE_PROXY_URL)
    require(
        endpoint.scheme == "https" ||
            (BuildConfig.DEBUG && endpoint.scheme == "http" && endpoint.host == "localhost"),
    )
    return endpoint
}

private fun readProxyResponse(url: Uri): Pair<JSONObject, Boolean> {
    val connection = URL(url.toString()).openConnection() as HttpURLConnection
    try {
        connection.instanceFollowRedirects = false
        connection.connectTimeout = 5_000
        connection.readTimeout = 12_000
        if (connection.responseCode != HttpURLConnection.HTTP_OK) throw IOException("Kakao proxy request failed")
        val isDemo = connection.getHeaderField("X-LetGoHome-Demo") == "1"
        val payload = connection.inputStream.bufferedReader().use { it.readText() }
        return JSONObject(payload) to isDemo
    } finally {
        connection.disconnect()
    }
}

internal fun parseDrivingRoute(response: JSONObject, isDemo: Boolean): DrivingRoute {
    val route = response.optJSONArray("routes")?.optJSONObject(0) ?: throw RouteUnavailable()
    if (route.optInt("result_code", -1) != 0) throw RouteUnavailable()
    val summary = route.getJSONObject("summary")
    val distance = summary.getInt("distance")
    val duration = summary.getInt("duration")
    require(distance >= 0 && duration >= 0)

    // Keep each road separate so a gap in Kakao's geometry is never drawn as a straight segment.
    val paths = buildList {
        val sections = route.getJSONArray("sections")
        for (sectionIndex in 0 until sections.length()) {
            val roads = sections.getJSONObject(sectionIndex).optJSONArray("roads") ?: continue
            for (roadIndex in 0 until roads.length()) {
                val vertices = roads.getJSONObject(roadIndex).optJSONArray("vertexes") ?: continue
                if (vertices.length() < 4 || vertices.length() % 2 != 0) continue
                add((0 until vertices.length() step 2).map { index ->
                    GeoPoint(vertices.getDouble(index + 1), vertices.getDouble(index))
                })
            }
        }
    }
    if (paths.isEmpty()) throw RouteUnavailable()
    return DrivingRoute(distance, duration, paths, isDemo)
}

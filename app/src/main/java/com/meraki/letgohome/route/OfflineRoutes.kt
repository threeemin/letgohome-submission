package com.meraki.letgohome.route

import java.util.Locale

internal object OfflineRoutes {
    val origin = GeoPoint(37.5664, 126.9769)

    private data class Sample(
        val place: DestinationPlace,
        val alias: String,
        val distanceMeters: Int,
        val durationSeconds: Int,
    )

    private val samples = listOf(
        Sample(DestinationPlace("서울역", "서울 중구 한강대로 405", GeoPoint(37.5547, 126.9718)), "Seoul Station", 2_920, 540),
        Sample(DestinationPlace("한강공원", "서울 영등포구 여의동로 330", GeoPoint(37.5284, 126.9331)), "Hangang Park", 9_600, 1_380),
    )

    val defaultDestination: DestinationPlace
        get() = samples.first().place

    fun search(query: String): List<DestinationPlace> {
        val term = query.trim().lowercase(Locale.ROOT).replace(" ", "")
        if (term.isEmpty()) return emptyList()
        return samples.filter { sample ->
            listOf(sample.place.name, sample.place.address, sample.alias)
                .any { term in it.lowercase(Locale.ROOT).replace(" ", "") }
        }.map { it.place }
    }

    fun route(origin: GeoPoint, destination: GeoPoint): DrivingRoute {
        val sample = samples.firstOrNull { it.place.point == destination } ?: throw RouteUnavailable()
        val midpoint = (origin.latitude + destination.latitude) / 2
        return DrivingRoute(
            distanceMeters = sample.distanceMeters,
            durationSeconds = sample.durationSeconds,
            paths = listOf(listOf(
                origin,
                GeoPoint(midpoint, origin.longitude),
                GeoPoint(midpoint, destination.longitude),
                destination,
            )),
            isDemo = true,
        )
    }
}

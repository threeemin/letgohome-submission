package com.meraki.letgohome.route

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import androidx.activity.compose.LocalActivity
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.LifecycleOwner
import com.kakao.vectormap.KakaoMap
import com.kakao.vectormap.KakaoMapReadyCallback
import com.kakao.vectormap.KakaoMapSdk
import com.kakao.vectormap.LatLng
import com.kakao.vectormap.MapLifeCycleCallback
import com.kakao.vectormap.MapView
import com.kakao.vectormap.camera.CameraUpdateFactory
import com.kakao.vectormap.label.LabelOptions
import com.kakao.vectormap.label.LabelStyle
import com.kakao.vectormap.label.LabelStyles
import com.kakao.vectormap.route.RouteLineOptions
import com.kakao.vectormap.route.RouteLineSegment
import com.kakao.vectormap.route.RouteLineStyle
import com.kakao.vectormap.route.RouteLineStyles
import com.kakao.vectormap.route.RouteLineStylesSet
import com.meraki.letgohome.BuildConfig
import com.meraki.letgohome.ui.CampBlue
import com.meraki.letgohome.ui.CampWarning
import kotlin.math.roundToInt

@Composable
internal fun KakaoRouteMap(
    origin: GeoPoint?,
    destination: GeoPoint?,
    route: DrivingRoute?,
    onDestination: (GeoPoint) -> Unit,
    onError: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val lifecycle = (LocalActivity.current as? LifecycleOwner)?.lifecycle
    val tap by rememberUpdatedState(onDestination)
    val reportError by rememberUpdatedState(onError)
    val startPoint by rememberUpdatedState(origin)
    val view = remember { MapView(context) }
    var map by remember { mutableStateOf<KakaoMap?>(null) }

    var viewport by remember { mutableStateOf(IntSize.Zero) }
    AndroidView(factory = { view }, modifier = modifier.onSizeChanged { viewport = it })
    DisposableEffect(view, lifecycle) {
        var disposed = false
        var started = false
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_RESUME -> view.resume()
                Lifecycle.Event.ON_PAUSE -> view.pause()
                else -> Unit
            }
        }
        try {
            KakaoMapSdk.init(context.applicationContext, BuildConfig.KAKAO_MAP_NATIVE_APP_KEY)
            view.start(
                object : MapLifeCycleCallback() {
                    override fun onMapDestroy() = Unit
                    override fun onMapError(error: Exception) {
                        view.post {
                            if (!disposed) {
                                map = null
                                reportError()
                            }
                        }
                    }
                },
                object : KakaoMapReadyCallback() {
                    override fun getPosition(): LatLng = (startPoint ?: SeoulCenter).asLatLng()
                    override fun getZoomLevel(): Int = 15

                    override fun onMapReady(kakaoMap: KakaoMap) {
                        view.post {
                            if (!disposed) {
                                kakaoMap.setOnMapClickListener { _, position, _, _ ->
                                    view.post {
                                        if (!disposed) {
                                            tap(GeoPoint(position.latitude, position.longitude))
                                        }
                                    }
                                }
                                map = kakaoMap
                            }
                        }
                    }
                },
            )
            started = true
            view.setFinishManually(true)
            lifecycle?.addObserver(observer)
        } catch (_: RuntimeException) {
            reportError()
        }
        onDispose {
            disposed = true
            lifecycle?.removeObserver(observer)
            map = null
            if (started) view.finish()
        }
    }

    val density = context.resources.displayMetrics.density
    val markerSize = (18 * density).roundToInt()
    val originIcon = remember(markerSize) { marker(markerSize, CampBlue.toArgb()) }
    val destinationIcon = remember(markerSize) { marker(markerSize, CampWarning.toArgb()) }

    LaunchedEffect(map, origin, destination, route) {
        val kakaoMap = map ?: return@LaunchedEffect
        kakaoMap.labelManager?.layer?.apply {
            removeAll()
            origin?.let {
                addLabel(LabelOptions.from(it.asLatLng()).setStyles(LabelStyles.from(LabelStyle.from(originIcon))))
            }
            destination?.let {
                addLabel(LabelOptions.from(it.asLatLng()).setStyles(LabelStyles.from(LabelStyle.from(destinationIcon))))
            }
        }
        val layer = kakaoMap.routeLineManager?.layer
        layer?.removeAll()
        if (route != null && destination != null && origin != null) {
            val style = RouteLineStylesSet.from(
                "return-route",
                RouteLineStyles.from(RouteLineStyle.from(5 * density, CampBlue.toArgb())),
            )
            val segments = route.paths.map { road ->
                RouteLineSegment.from(road.map { it.asLatLng() }).setStyles(style.getStyles(0))
            }
            layer?.addRouteLine(RouteLineOptions.from(segments).setStylesSet(style))
        }
    }

    LaunchedEffect(map, origin, destination, route, viewport) {
        val kakaoMap = map ?: return@LaunchedEffect
        if (viewport == IntSize.Zero) return@LaunchedEffect
        if (route != null && destination != null && origin != null) {
            val bounds = route.paths.flatten() + listOf(origin, destination)
            val padding = minOf((32 * density).roundToInt(), viewport.height / 6, viewport.width / 6)
            kakaoMap.moveCamera(CameraUpdateFactory.fitMapPoints(
                bounds.map { it.asLatLng() }.toTypedArray(), padding, 17,
            ))
        } else if (destination == null && origin != null) {
            kakaoMap.moveCamera(CameraUpdateFactory.newCenterPosition(origin.asLatLng(), 15))
        }
    }
}

private val SeoulCenter = GeoPoint(37.5665, 126.9780)

private fun GeoPoint.asLatLng(): LatLng = LatLng.from(latitude, longitude)

private fun marker(size: Int, color: Int): Bitmap {
    val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
    val center = size / 2f
    val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    val canvas = Canvas(bitmap)
    paint.color = android.graphics.Color.WHITE
    canvas.drawCircle(center, center, center, paint)
    paint.color = color
    canvas.drawCircle(center, center, center * 0.72f, paint)
    return bitmap
}

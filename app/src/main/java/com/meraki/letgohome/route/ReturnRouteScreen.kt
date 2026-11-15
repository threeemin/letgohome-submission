package com.meraki.letgohome.route

import android.Manifest
import android.content.res.Configuration
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.meraki.letgohome.BuildConfig
import com.meraki.letgohome.ui.CampBlue
import com.meraki.letgohome.ui.CampSand
import com.meraki.letgohome.ui.CampWarning
import com.meraki.letgohome.ui.EnergyPanel
import com.meraki.letgohome.ui.displayAmount
import com.meraki.letgohome.ui.displayDuration
import kotlinx.coroutines.CancellationException

internal data class RouteSelection(
    val origin: GeoPoint,
    val destination: GeoPoint,
    val route: DrivingRoute,
    val destinationName: String? = null,
)

private data class RouteRequest(val origin: GeoPoint, val destination: GeoPoint, val name: String?)

private val DemoOrigin = GeoPoint(37.50, 127.05)
private val DemoDestination = GeoPoint(37.30, 127.10)

@Composable
internal fun ReturnRouteScreen(
    previous: RouteSelection?,
    reservedEnergyKwh: Double?,
    onRoute: (RouteSelection) -> Unit,
    plan: @Composable () -> Unit,
    appHeader: @Composable () -> Unit,
    navigation: @Composable () -> Unit,
) {
    val context = LocalContext.current
    val demo = BuildConfig.DEBUG && BuildConfig.KAKAO_MAP_NATIVE_APP_KEY.isBlank()
    val enabled = BuildConfig.ROUTE_PROXY_URL.isNotBlank() &&
        (demo || BuildConfig.KAKAO_MAP_NATIVE_APP_KEY.isNotBlank())
    var origin by remember { mutableStateOf(if (demo) DemoOrigin else previous?.origin) }
    val route = previous?.route
    val destination = previous?.destination
    var request by remember { mutableStateOf<RouteRequest?>(null) }
    var pendingDestination by remember { mutableStateOf<Pair<GeoPoint, String?>?>(null) }
    var locationAttempt by remember {
        mutableStateOf(if (enabled && !demo && previous == null && context.hasForegroundLocation()) 1 else 0)
    }
    var locating by remember { mutableStateOf(false) }
    var mapFailed by remember { mutableStateOf(false) }
    var message by remember { mutableStateOf<String?>(null) }

    fun refreshLocation() {
        locating = true
        request = null
        locationAttempt++
    }

    val permission = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions(),
    ) {
        if (context.hasForegroundLocation()) refreshLocation()
        else {
            pendingDestination = null
            message = "위치 권한을 허용한 뒤 현위치를 가져올 수 있습니다."
        }
    }
    fun getLocation() {
        if (context.hasForegroundLocation()) refreshLocation()
        else permission.launch(arrayOf(
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION,
        ))
    }

    fun queueRoute(from: GeoPoint, point: GeoPoint, name: String?) {
        if (from == point) {
            message = "현위치와 다른 곳을 목적지로 선택해 주세요."
        } else {
            message = null
            request = RouteRequest(from, point, name)
        }
    }

    LaunchedEffect(locationAttempt) {
        if (locationAttempt == 0) return@LaunchedEffect
        locating = true
        message = null
        val current = currentPosition(context)
        locating = false
        if (current == null) {
            message = "현위치를 가져오지 못했습니다. 위치 설정을 확인하고 다시 시도해 주세요."
        } else {
            origin = current
            val target = pendingDestination
                ?: previous?.let { it.destination to it.destinationName }
            target?.let { (point, name) -> queueRoute(current, point, name) }
        }
        pendingDestination = null
    }

    fun selectDestination(point: GeoPoint, name: String? = null) {
        val current = origin
        if (current == null || locating) {
            pendingDestination = point to name
            if (!locating) getLocation()
        } else {
            queueRoute(current, point, name)
        }
    }

    LaunchedEffect(request) {
        val (from, to, name) = request ?: return@LaunchedEffect
        try {
            val result = loadDrivingRoute(from, to, demoOnly = demo)
            onRoute(RouteSelection(from, to, result, name))
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (_: RouteUnavailable) {
            message = "자동차 경로를 찾지 못했습니다. 도로 근처의 다른 지점을 선택해 주세요."
        } catch (_: Exception) {
            message = "경로를 가져오지 못했습니다. 연결을 확인하고 다시 시도해 주세요."
        }
        request = null
    }

    val showMap = enabled && !mapFailed
    RoutePageLayout(
        showMap = showMap,
        route = route,
        appHeader = appHeader,
        navigation = navigation,
        title = { Text("귀가 경로", style = MaterialTheme.typography.headlineSmall) },
        search = {
            if (enabled) {
                DestinationSearch(demo, previous?.destinationName) { place ->
                    selectDestination(place.point, place.name)
                }
            }
        },
        map = {
            val mapModifier = Modifier.fillMaxSize().clip(MaterialTheme.shapes.large)
            when {
                !enabled -> EnergyPanel {
                    Text("지도와 길찾기 설정을 확인해 주세요. 귀가 계획에서 거리를 직접 입력할 수 있습니다.")
                }
                mapFailed -> EnergyPanel {
                    Text("카카오 지도를 열지 못했습니다. 앱 인증 또는 네트워크를 확인해 주세요.")
                }
                demo -> DemoRouteDiagram(route, { selectDestination(DemoDestination) }, mapModifier)
                else -> KakaoRouteMap(
                    origin = previous?.origin ?: origin,
                    destination = destination,
                    route = route,
                    onDestination = { selectDestination(it) },
                    onError = { mapFailed = true },
                    modifier = mapModifier,
                )
            }
        },
        details = {
            route?.let { RouteSummary(it, reservedEnergyKwh) }
            if (enabled && !demo) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                    Button(
                        enabled = !locating,
                        onClick = ::getLocation,
                    ) { Text(if (origin == null) "현위치 가져오기" else "현위치 새로고침") }
                    if (mapFailed) OutlinedButton(onClick = { mapFailed = false }) { Text("지도 다시 열기") }
                }
            }
            if (locating || request != null) {
                LinearProgressIndicator(Modifier.fillMaxWidth())
                Text(if (locating) "현위치를 가져오는 중" else "자동차 경로를 찾는 중")
            }
            message?.let { Text(it, color = CampWarning) }
        },
        plan = plan,
    )
}

@Composable
private fun RoutePageLayout(
    showMap: Boolean,
    route: DrivingRoute?,
    appHeader: @Composable () -> Unit,
    navigation: @Composable () -> Unit,
    title: @Composable () -> Unit,
    search: @Composable () -> Unit,
    map: @Composable () -> Unit,
    details: @Composable () -> Unit,
    plan: @Composable () -> Unit,
) {
    val landscape = LocalConfiguration.current.orientation == Configuration.ORIENTATION_LANDSCAPE
    BoxWithConstraints(
        modifier = Modifier.fillMaxSize().windowInsetsPadding(WindowInsets.safeDrawing).imePadding(),
        contentAlignment = Alignment.TopCenter,
    ) {
        if (showMap && landscape && maxWidth >= 560.dp) {
            val narrowLandscape = maxWidth < 700.dp
            val detailsWidth = (maxWidth * 0.36f).coerceIn(256.dp, 360.dp)
            val detailsScroll = rememberScrollState()
            LaunchedEffect(route) {
                if (route != null) detailsScroll.scrollTo(0)
            }
            Column(
                Modifier.widthIn(max = 1120.dp).fillMaxSize().padding(horizontal = 16.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Box(Modifier.weight(1f)) { appHeader() }
                    val navigationModifier = if (narrowLandscape) {
                        Modifier.width(216.dp)
                    } else {
                        Modifier.weight(1.4f)
                    }
                    Box(navigationModifier) { navigation() }
                }
                Row(
                    Modifier.weight(1f).fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Column(
                        Modifier.weight(1f).fillMaxHeight(),
                        verticalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        if (!narrowLandscape || route == null) title()
                        search()
                        Box(Modifier.weight(1f)) { map() }
                    }
                    Column(
                        Modifier.width(detailsWidth).fillMaxHeight().verticalScroll(detailsScroll),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        details()
                        plan()
                    }
                }
            }
            return@BoxWithConstraints
        }

        val verticalPadding = 12.dp
        val viewportHeight = maxHeight
        Layout(
            modifier = Modifier.widthIn(max = 1120.dp).fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = verticalPadding),
            content = {
                Column(
                    Modifier.padding(bottom = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    appHeader()
                    navigation()
                    title()
                    search()
                }
                Box(propagateMinConstraints = true) { map() }
                Column(
                    Modifier.padding(top = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) { details() }
                Box(Modifier.padding(top = 12.dp)) { plan() }
            },
        ) { (headerContent, mapContent, detailsContent, planContent), constraints ->
            val contentConstraints = constraints.copy(minHeight = 0)
            val headerPlaceable = headerContent.measure(contentConstraints)
            val detailsPlaceable = detailsContent.measure(contentConstraints)
            val mapConstraints = if (showMap) {
                // Reserve space for the measured text and controls before sizing the map.
                // On very short screens, keep a usable map and let the page scroll.
                val contentHeight = viewportHeight.roundToPx() - 2 * verticalPadding.roundToPx()
                val mapHeight = (contentHeight - headerPlaceable.height - detailsPlaceable.height)
                    .coerceIn(144.dp.roundToPx(), 440.dp.roundToPx())
                contentConstraints.copy(minHeight = mapHeight, maxHeight = mapHeight)
            } else {
                contentConstraints
            }
            val mapPlaceable = mapContent.measure(mapConstraints)
            val detailsTop = headerPlaceable.height + mapPlaceable.height
            val planTop = detailsTop + detailsPlaceable.height
            val planPlaceable = planContent.measure(contentConstraints)
            layout(constraints.maxWidth, planTop + planPlaceable.height) {
                headerPlaceable.placeRelative(0, 0)
                mapPlaceable.placeRelative(0, headerPlaceable.height)
                detailsPlaceable.placeRelative(0, detailsTop)
                planPlaceable.placeRelative(0, planTop)
            }
        }
    }
}

@Composable
private fun RouteSummary(route: DrivingRoute, reservedEnergyKwh: Double?) {
    val reservedEnergy = reservedEnergyKwh.displayAmount("kWh").replace(' ', '\u00A0')
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = CampSand,
        contentColor = CampBlue,
        shape = MaterialTheme.shapes.large,
    ) {
        Column(
            Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Text(
                "귀가 거리 ${(route.distanceMeters / 1000.0).displayAmount("km")}",
                style = MaterialTheme.typography.titleMedium,
            )
            Text("예상 소요 시간 ${(route.durationSeconds / 3600.0).displayDuration()}", style = MaterialTheme.typography.bodyMedium)
            Text("귀가용 전력 $reservedEnergy", style = MaterialTheme.typography.bodyMedium)
            Text(
                if (route.isDemo) "시연 경로 · 예시 거리와 시간" else "카카오 자동차 길찾기",
                style = MaterialTheme.typography.labelMedium,
            )
        }
    }
}

@Composable
private fun DemoRouteDiagram(
    route: DrivingRoute?,
    onSelect: () -> Unit,
    modifier: Modifier,
) {
    Box(
        modifier.background(CampSand).clickable(onClickLabel = "시연 목적지 선택", onClick = onSelect),
    ) {
        Canvas(Modifier.fillMaxSize()) {
            fun project(point: GeoPoint): Offset {
                val east = (point.longitude - DemoOrigin.longitude) /
                    (DemoDestination.longitude - DemoOrigin.longitude)
                val south = (point.latitude - DemoOrigin.latitude) /
                    (DemoDestination.latitude - DemoOrigin.latitude)
                return Offset(size.width * (0.18f + 0.64f * east.toFloat()), size.height * (0.28f + 0.44f * south.toFloat()))
            }
            route?.paths?.forEach { path ->
                path.zipWithNext().forEach { (a, b) ->
                    drawLine(CampBlue, project(a), project(b), strokeWidth = 5.dp.toPx(), cap = StrokeCap.Round)
                }
            }
            drawCircle(CampBlue, radius = 8.dp.toPx(), center = project(DemoOrigin))
            drawCircle(CampWarning, radius = 8.dp.toPx(), center = project(DemoDestination))
        }
        Text("가상 출발지", Modifier.align(Alignment.TopStart).padding(16.dp), color = CampBlue)
        Text("시연 목적지 선택", Modifier.align(Alignment.BottomEnd).padding(16.dp), color = CampBlue)
    }
}

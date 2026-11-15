package com.meraki.letgohome.ui

import android.content.Context
import androidx.activity.compose.BackHandler
import androidx.activity.compose.LocalActivity
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.core.content.edit
import androidx.core.view.WindowCompat
import com.meraki.letgohome.contract.BatterySnapshot
import com.meraki.letgohome.contract.DemoDefaults
import com.meraki.letgohome.contract.EnergyEstimate
import com.meraki.letgohome.contract.EnergyInputs
import com.meraki.letgohome.contract.EnergyStatus
import com.meraki.letgohome.route.ReturnRouteScreen
import com.meraki.letgohome.route.RouteSelection

private val StringListSaver = listSaver<List<String>, String>(
    save = { it },
    restore = { it.toList() },
)

@Composable
internal fun LetGoHomeApp(
    battery: BatterySnapshot,
    calculate: (EnergyInputs) -> EnergyEstimate,
    selectedRoute: MutableState<RouteSelection?> = remember { mutableStateOf(null) },
) {
    var values by rememberSaveable(stateSaver = StringListSaver) { mutableStateOf(defaultValues()) }
    var manualFields by rememberSaveable(stateSaver = StringListSaver) { mutableStateOf(emptyList<String>()) }
    var page by rememberSaveable { mutableStateOf(CampPage.HOME) }
    val dashboard = page == CampPage.POWER
    var campingActive by rememberSaveable { mutableStateOf(false) }
    var pickedRoute by selectedRoute
    val routeDuration = pickedRoute?.route?.durationSeconds
    val routeIsDemo = pickedRoute?.route?.isDemo == true
    var showInputErrors by rememberSaveable { mutableStateOf(false) }
    var lastDashboardStatus by rememberSaveable { mutableStateOf<String?>(null) }
    var showAlert by rememberSaveable { mutableStateOf(false) }
    var departureDraft by rememberSaveable { mutableStateOf<String?>(null) }
    val input = values.toInputs(battery.socPercent)
    val estimate = remember(input, calculate) { calculate(input) }
    val context = LocalContext.current
    val profile = remember(context) { context.getSharedPreferences("profile", Context.MODE_PRIVATE) }
    var vehicleName by remember(profile) { mutableStateOf(profile.getString("vehicle", null) ?: DefaultVehicleName) }
    var displayId by remember(profile) { mutableStateOf(profile.getString("display_id", null) ?: DefaultDisplayId) }
    val focus = LocalFocusManager.current
    val view = LocalView.current
    val window = LocalActivity.current?.window
    val settingsScroll = rememberScrollState()
    val dashboardScroll = rememberScrollState()
    val homeScroll = rememberScrollState()

    fun updateField(field: PlanField, value: String) {
        values = values.toMutableList().apply { set(field.ordinal, value) }
        if (field == PlanField.DISTANCE) {
            pickedRoute = null
        }
        if (field.name !in manualFields) {
            manualFields = manualFields + field.name
        }
    }

    fun openPage(target: CampPage) {
        if (target == page) return
        focus.clearFocus()
        page = target
        showAlert = false
        lastDashboardStatus = null
    }

    fun openRoute() = openPage(CampPage.RETURN)

    fun startCamping() {
        val hasFieldErrors = PlanField.entries.any {
            it.key in estimate.invalidFields || values[it.ordinal].toInputNumber() == null
        }
        showInputErrors = hasFieldErrors
        if (hasFieldErrors) {
            openRoute()
        } else {
            campingActive = true
            openPage(CampPage.HOME)
        }
    }

    fun editDeparture() {
        showAlert = false
        departureDraft = values[PlanField.DEPARTURE.ordinal]
    }

    LaunchedEffect(campingActive) { homeScroll.scrollTo(0) }

    LaunchedEffect(dashboard, estimate.status) {
        if (dashboard && lastDashboardStatus != estimate.status.name) {
            lastDashboardStatus = estimate.status.name
            showAlert = estimate.status.isWarning
        }
    }

    DisposableEffect(window, view) {
        val controller = window?.let { WindowCompat.getInsetsController(it, view) }
        val previousAppearance = controller?.isAppearanceLightStatusBars
        val previousNavigationAppearance = controller?.isAppearanceLightNavigationBars
        controller?.isAppearanceLightStatusBars = true
        controller?.isAppearanceLightNavigationBars = true
        onDispose {
            if (previousAppearance != null) controller.isAppearanceLightStatusBars = previousAppearance
            if (previousNavigationAppearance != null) controller.isAppearanceLightNavigationBars = previousNavigationAppearance
        }
    }

    BackHandler(enabled = (campingActive || page != CampPage.HOME) && departureDraft == null && !showAlert) {
        if (page == CampPage.HOME) campingActive = false else openPage(CampPage.HOME)
    }

    MaterialTheme(colorScheme = WarmCampColors, shapes = WarmCampShapes) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = CampCanvas,
            contentColor = MaterialTheme.colorScheme.onSurface,
        ) {
            if (page == CampPage.RETURN) {
                ReturnRouteScreen(
                    previous = pickedRoute,
                    reservedEnergyKwh = estimate.reservedEnergyKwh,
                    onRoute = { selection ->
                        pickedRoute = selection
                        values = values.toMutableList().apply {
                            set(PlanField.DISTANCE.ordinal, (selection.route.distanceMeters / 1000.0).toString())
                        }
                        manualFields = manualFields - PlanField.DISTANCE.name
                    },
                    plan = {
                        ReturnPlanFields(
                            values = values,
                            manualFields = manualFields,
                            hasRoute = pickedRoute != null,
                            invalidFields = estimate.invalidFields,
                            showInputErrors = showInputErrors,
                            onValueChange = ::updateField,
                        )
                    },
                    appHeader = { CampHeader(battery) },
                    navigation = { CampNavigation(CampPage.RETURN, ::openPage) },
                )
            } else {
                BoxWithConstraints(
                    modifier = Modifier
                        .fillMaxSize()
                        .windowInsetsPadding(WindowInsets.safeDrawing)
                        .imePadding(),
                    contentAlignment = Alignment.TopCenter,
                ) {
                    val sceneHeight = (maxHeight - 108.dp).coerceAtLeast(232.dp)
                    val shortWindow = page == CampPage.HOME && !campingActive && maxHeight < 440.dp
                    val scroll = when (page) {
                        CampPage.HOME -> homeScroll
                        CampPage.POWER -> dashboardScroll
                        CampPage.SETTINGS -> settingsScroll
                        CampPage.RETURN -> error("Route is shown separately")
                    }
                    Column(
                        modifier = Modifier
                            .widthIn(max = 1120.dp)
                            .fillMaxSize()
                            .verticalScroll(scroll)
                            .padding(horizontal = 16.dp, vertical = if (shortWindow) 4.dp else 12.dp),
                        verticalArrangement = Arrangement.spacedBy(if (shortWindow) 6.dp else 14.dp),
                    ) {
                        CampHeader(battery)
                        CampNavigation(
                            selected = page,
                            onSelect = ::openPage,
                        )
                        when (page) {
                            CampPage.HOME -> if (campingActive) {
                                CampSession(
                                    battery = battery,
                                    estimate = estimate,
                                    sceneHeight = sceneHeight,
                                    onReturnPlan = ::openRoute,
                                    onFinish = { campingActive = false },
                                )
                            } else {
                                WarmCampHome(battery, estimate, shortWindow, ::startCamping, ::openRoute)
                            }
                            CampPage.POWER -> EnergyDashboard(
                                battery = battery,
                                vehicleName = vehicleName,
                                input = input,
                                estimate = estimate,
                                manualFields = manualFields,
                                routeDurationSeconds = routeDuration,
                                routeIsDemo = routeIsDemo,
                                onReturnPlan = ::openRoute,
                                onDeparture = ::editDeparture,
                                onIncrease = {
                                    values = values.toMutableList().apply { set(PlanField.POWER.ordinal, "2.5") }
                                    manualFields = manualFields - PlanField.POWER.name
                                },
                                onReset = {
                                    values = defaultValues()
                                    manualFields = emptyList()
                                    pickedRoute = null
                                },
                            )
                            CampPage.SETTINGS -> PersonalSettings(
                                vehicleName = vehicleName,
                                displayId = displayId,
                                onVehicleChange = {
                                    vehicleName = it
                                    profile.edit { putString("vehicle", it) }
                                },
                                onDisplayIdChange = {
                                    displayId = it
                                    profile.edit { putString("display_id", it) }
                                },
                            )
                            CampPage.RETURN -> Unit
                        }
                    }
                }
            }
        }

        departureDraft?.let { draft ->
            DepartureDialog(
                draft = draft,
                onChange = { departureDraft = it },
                onDismiss = { departureDraft = null },
                onSave = {
                    updateField(PlanField.DEPARTURE, draft)
                    departureDraft = null
                },
            )
        }
        if (dashboard && departureDraft == null && showAlert && estimate.status.isWarning) {
            EnergyWarningDialog(
                estimate = estimate,
                onDeparture = ::editDeparture,
                onDismiss = { showAlert = false },
            )
        }
    }
}

internal enum class PlanField(val key: String, val label: String, val unit: String) {
    DISTANCE("returnDistanceKm", "귀가 거리", "km"),
    DEPARTURE("hoursUntilDeparture", "출발까지 남은 시간", "시간"),
    RESERVE("safetyReservePercentPoint", "안전 여유", "%p"),
    CAPACITY("batteryCapacityKwh", "배터리 용량", "kWh"),
    EFFICIENCY("efficiencyKmPerKwh", "전비", "km/kWh"),
    POWER("averagePowerKw", "평균 소비전력", "kW"),
}

private fun defaultValues(): List<String> = DemoDefaults.baseline.run {
    listOf(
        returnDistanceKm,
        hoursUntilDeparture,
        safetyReservePercentPoint,
        batteryCapacityKwh,
        efficiencyKmPerKwh,
        averagePowerKw,
    ).map { it?.toString()?.removeSuffix(".0").orEmpty() }
}

private fun List<String>.toInputs(socPercent: Double?) = EnergyInputs(
    socPercent = socPercent,
    batteryCapacityKwh = this[PlanField.CAPACITY.ordinal].toInputNumber(),
    returnDistanceKm = this[PlanField.DISTANCE.ordinal].toInputNumber(),
    efficiencyKmPerKwh = this[PlanField.EFFICIENCY.ordinal].toInputNumber(),
    safetyReservePercentPoint = this[PlanField.RESERVE.ordinal].toInputNumber(),
    averagePowerKw = this[PlanField.POWER.ordinal].toInputNumber(),
    hoursUntilDeparture = this[PlanField.DEPARTURE.ordinal].toInputNumber(),
)

internal fun String.toInputNumber(): Double? = trim().replace(',', '.').toDoubleOrNull()

internal val EnergyStatus.isWarning: Boolean
    get() = this == EnergyStatus.AT_RISK || this == EnergyStatus.INSUFFICIENT

@Composable
private fun ReturnPlanFields(
    values: List<String>,
    manualFields: List<String>,
    hasRoute: Boolean,
    invalidFields: List<String>,
    showInputErrors: Boolean,
    onValueChange: (PlanField, String) -> Unit,
) {
    var advanced by rememberSaveable { mutableStateOf(false) }
    val additionalFields = listOf(PlanField.DISTANCE, PlanField.CAPACITY, PlanField.EFFICIENCY, PlanField.POWER)

    fun hasError(field: PlanField) =
        field.key in invalidFields || (showInputErrors && values[field.ordinal].toInputNumber() == null)

    EnergyPanel {
        Text("귀가 계획", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        for (field in listOf(PlanField.DEPARTURE, PlanField.RESERVE)) {
            PlanInput(field, values[field.ordinal], hasError(field), { onValueChange(field, it) })
        }
        Text("안전 여유는 배터리 전체 용량 기준입니다.", style = MaterialTheme.typography.bodySmall)
        TextButton(onClick = { advanced = !advanced }) {
            Text(if (advanced) "상세 설정 접기" else "상세 설정 펼치기")
        }
        if (advanced || additionalFields.any(::hasError)) {
            for (field in additionalFields) {
                val source = when {
                    field.name in manualFields -> "직접 입력"
                    field == PlanField.DISTANCE && hasRoute -> "지도 경로"
                    else -> "시연값"
                }
                PlanInput(field, values[field.ordinal], hasError(field), { onValueChange(field, it) }, source)
            }
        }
    }
}

@Composable
internal fun PlanInput(
    field: PlanField,
    value: String,
    isError: Boolean,
    onChange: (String) -> Unit,
    source: String? = null,
    imeAction: ImeAction = ImeAction.Next,
) {
    OutlinedTextField(
        modifier = Modifier.fillMaxWidth(),
        value = value,
        onValueChange = onChange,
        singleLine = true,
        label = { Text(field.label) },
        suffix = { Text(field.unit) },
        isError = isError,
        supportingText = if (isError || source != null) {
            { Text(if (isError) "올바른 숫자와 범위를 입력해 주세요." else source.orEmpty()) }
        } else {
            null
        },
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal, imeAction = imeAction),
    )
}

@Composable
internal fun EnergyPanel(
    modifier: Modifier = Modifier,
    color: Color = CampCard,
    content: @Composable ColumnScope.() -> Unit,
) {
    Surface(modifier = modifier.fillMaxWidth(), color = color, contentColor = CampBlue, shape = MaterialTheme.shapes.large) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
            content = content,
        )
    }
}

@Composable
internal fun ResponsiveColumns(
    primary: @Composable () -> Unit,
    secondary: @Composable () -> Unit,
) {
    BoxWithConstraints(Modifier.fillMaxWidth()) {
        if (maxWidth >= 660.dp) {
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                Box(Modifier.weight(1.2f)) { primary() }
                Box(Modifier.weight(1f)) { secondary() }
            }
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                primary()
                secondary()
            }
        }
    }
}

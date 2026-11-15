package com.meraki.letgohome.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import com.meraki.letgohome.R
import com.meraki.letgohome.contract.BatterySnapshot
import com.meraki.letgohome.contract.ChargingState
import com.meraki.letgohome.contract.DataSource
import com.meraki.letgohome.contract.EnergyEstimate
import com.meraki.letgohome.contract.EnergyInputs
import com.meraki.letgohome.contract.EnergyStatus
import java.text.NumberFormat
import kotlin.math.min
import kotlin.math.roundToLong

@Composable
internal fun EnergyDashboard(
    battery: BatterySnapshot,
    vehicleName: String,
    input: EnergyInputs,
    estimate: EnergyEstimate,
    manualFields: List<String>,
    routeDurationSeconds: Int?,
    routeIsDemo: Boolean,
    onReturnPlan: () -> Unit,
    onDeparture: () -> Unit,
    onIncrease: () -> Unit,
    onReset: () -> Unit,
) {
    VehicleBatteryCard(battery, vehicleName, estimate)
    ResponsiveColumns(
        primary = {
            Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                EnergyPanel {
                    Text("전력 배분", style = MaterialTheme.typography.titleMedium)
                    EnergySplit(estimate)
                }
                EnergyPanel {
                    Text("계산에 사용한 값", style = MaterialTheme.typography.titleMedium)
                    val distanceSource = when {
                        routeDurationSeconds != null && routeIsDemo -> "시연 경로"
                        routeDurationSeconds != null -> "카카오 자동차 길찾기"
                        PlanField.DISTANCE.name in manualFields -> "직접 입력"
                        else -> "시연값"
                    }
                    Text("귀가 거리  ${input.returnDistanceKm.displayAmount("km")} · $distanceSource", color = CampBlue)
                    routeDurationSeconds?.let {
                        Text("예상 주행 시간 ${(it / 3600.0).displayDuration()}", color = CampBlue)
                    }
                    SourceValue(PlanField.CAPACITY, input.batteryCapacityKwh, manualFields)
                    SourceValue(PlanField.EFFICIENCY, input.efficiencyKmPerKwh, manualFields)
                    SourceValue(PlanField.POWER, input.averagePowerKw, manualFields)
                }
            }
        },
        secondary = {
            Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                EnergyPanel(color = CampSand) {
                    Text("예상 차박 가능 시간", style = MaterialTheme.typography.titleMedium)
                    Text(estimate.remainingHours.displayDuration(), style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.Bold)
                    Text("현재 소비 조건 기준 예상", color = CampBlue)
                    Text("출발까지 ${input.hoursUntilDeparture.displayDuration(approximate = false)}", color = CampBlue)
                }
                EnergyPanel(color = if (estimate.status.isWarning) CampWarningSurface else CampCard) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        if (estimate.status.isWarning) {
                            CampGlyph(CampIcon.WARNING, Modifier.size(22.dp), CampWarning)
                        }
                        Text(
                            text = estimate.status.title,
                            style = MaterialTheme.typography.titleMedium,
                            color = if (estimate.status.isWarning) CampWarning else CampBlue,
                        )
                    }
                    estimate.shortfallLabel()?.let { Text(it, color = CampWarning) }
                    when (estimate.status) {
                        EnergyStatus.MISSING_DATA -> Text("배터리 데이터와 계획 입력값을 확인해 주세요.", color = CampBlue)
                        EnergyStatus.INVALID_INPUT -> Text("귀가 계획에서 잘못된 입력값을 수정해 주세요.", color = CampBlue)
                        else -> Unit
                    }
                    Button(onDeparture, Modifier.fillMaxWidth().heightIn(min = 48.dp)) { Text("출발 시간 변경") }
                    TextButton(onReturnPlan, Modifier.fillMaxWidth().heightIn(min = 48.dp)) { Text("귀가 계획 수정") }
                    if (battery.source == DataSource.DEMO) {
                        OutlinedButton(onIncrease, Modifier.fillMaxWidth().heightIn(min = 48.dp)) {
                            Text("시연: 소비전력 증가")
                        }
                        TextButton(onReset, Modifier.fillMaxWidth().heightIn(min = 48.dp)) { Text("시연 초기화") }
                    }
                }
            }
        },
    )
}

@Composable
private fun VehicleBatteryCard(battery: BatterySnapshot, vehicleName: String, estimate: EnergyEstimate) {
    val soc = battery.socPercent?.takeIf { it.isFinite() && it in 0.0..100.0 }
    val source = when (battery.source) {
        DataSource.VEHICLE -> "차량 데이터"
        DataSource.MANUAL -> "직접 입력"
        DataSource.DEMO -> "시연 데이터"
    }
    val charging = when (battery.charging) {
        ChargingState.CHARGING -> "충전 중"
        ChargingState.NOT_CHARGING -> "충전 중 아님"
        ChargingState.UNKNOWN -> "확인 불가"
    }
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.extraLarge,
        color = Color(0xFF0A213A),
        contentColor = Color.White,
    ) {
        BoxWithConstraints(Modifier.fillMaxWidth().padding(16.dp)) {
            val photo: @Composable (Modifier) -> Unit = { modifier ->
                if (vehicleName == DefaultVehicleName || vehicleName.equals("IONIQ 5", ignoreCase = true)) {
                    Image(
                        painterResource(R.drawable.ioniq5_profile),
                        contentDescription = "아이오닉 5 측면",
                        modifier = modifier.clip(MaterialTheme.shapes.large).background(Color(0xFFE9F0F6)),
                        contentScale = ContentScale.Fit,
                    )
                } else {
                    Box(
                        modifier.clip(MaterialTheme.shapes.large).background(Color(0xFF173653)),
                        contentAlignment = Alignment.Center,
                    ) { CampGlyph(CampIcon.CAR, Modifier.size(76.dp), Color(0xFFB5DAF5)) }
                }
            }
            val metrics: @Composable (Modifier) -> Unit = { modifier ->
                Column(modifier, verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("내 자동차 · $source", style = MaterialTheme.typography.labelLarge, color = Color(0xFFB5DAF5))
                    Text(vehicleName, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                        CampGlyph(CampIcon.BATTERY, Modifier.size(22.dp), Color(0xFF7CDCCC))
                        Text("현재 배터리", style = MaterialTheme.typography.titleMedium)
                    }
                    Text(soc.displayAmount("%"), style = MaterialTheme.typography.displaySmall, fontWeight = FontWeight.Bold)
                    if (soc != null) {
                        LinearProgressIndicator(
                            progress = { (soc / 100.0).toFloat() },
                            modifier = Modifier.fillMaxWidth().height(8.dp),
                            color = Color(0xFF7CDCCC),
                            trackColor = Color(0xFF34516C),
                        )
                    }
                    Text("충전 상태 · $charging", style = MaterialTheme.typography.bodyLarge)
                    Text("배터리 에너지 ${estimate.currentEnergyKwh.displayAmount("kWh")}", style = MaterialTheme.typography.bodyMedium)
                }
            }
            if (maxWidth >= 480.dp) {
                Row(horizontalArrangement = Arrangement.spacedBy(20.dp), verticalAlignment = Alignment.CenterVertically) {
                    photo(Modifier.weight(1.2f).height(205.dp))
                    metrics(Modifier.weight(1f))
                }
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    photo(Modifier.fillMaxWidth().height(172.dp))
                    metrics(Modifier.fillMaxWidth())
                }
            }
        }
    }
}

@Composable
internal fun EnergySplit(estimate: EnergyEstimate) {
    val available = estimate.currentEnergyKwh
    val reserve = estimate.reservedEnergyKwh
    val camping = estimate.campingEnergyKwh
    if (available != null && available > 0 && reserve != null && camping != null) {
        val reserveRatio = (min(available, reserve) / available).toFloat().coerceIn(0f, 1f)
        val campingRatio = 1f - reserveRatio
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(12.dp)
                .clip(RoundedCornerShape(6.dp))
                .semantics { contentDescription = "현재 배터리 중 귀가용과 차박용 전력 비율" },
        ) {
            if (reserveRatio > 0f) Box(Modifier.weight(reserveRatio).height(12.dp).background(CampBlue))
            if (campingRatio > 0f) Box(Modifier.weight(campingRatio).height(12.dp).background(CampBlue.copy(alpha = 0.48f)))
        }
    }
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        EnergyValue("귀가에 남겨둘 전력", reserve, Modifier.weight(1f))
        EnergyValue("차박에 사용할 전력", camping, Modifier.weight(1f))
    }
}

@Composable
private fun EnergyValue(label: String, value: Double?, modifier: Modifier) {
    Column(modifier, verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(label, style = MaterialTheme.typography.bodyMedium)
        Text(value.displayAmount("kWh"), style = MaterialTheme.typography.headlineSmall)
    }
}

@Composable
private fun SourceValue(field: PlanField, value: Double?, manualFields: List<String>) {
    val source = if (field.name in manualFields) "직접 입력" else "시연값"
    Text("${field.label}  ${value.displayAmount(field.unit)} · $source", color = CampBlue)
}

@Composable
internal fun EnergyWarningDialog(
    estimate: EnergyEstimate,
    onDeparture: () -> Unit,
    onDismiss: () -> Unit,
) {
    val message = if (estimate.status == EnergyStatus.INSUFFICIENT) {
        "현재 귀가용 전력이 부족합니다."
    } else {
        "현재 소비가 계속되면 출발 전 귀가용 전력이 부족해질 것으로 예상됩니다."
    }
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = { CampGlyph(CampIcon.WARNING, Modifier.size(32.dp), CampWarning) },
        title = { Text(estimate.status.title) },
        containerColor = CampWarningSurface,
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(message)
                estimate.shortfallLabel()?.let {
                    Text(it, color = CampWarning, style = MaterialTheme.typography.titleLarge)
                }
            }
        },
        confirmButton = { TextButton(onDismiss, Modifier.heightIn(min = 48.dp)) { Text("확인") } },
        dismissButton = { TextButton(onDeparture, Modifier.heightIn(min = 48.dp)) { Text("출발 시간 변경") } },
    )
}

@Composable
internal fun DepartureDialog(
    draft: String,
    onChange: (String) -> Unit,
    onDismiss: () -> Unit,
    onSave: () -> Unit,
) {
    val valid = draft.toInputNumber()?.let { it.isFinite() && it >= 0 } == true
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("출발 시간 변경") },
        text = {
            PlanInput(
                field = PlanField.DEPARTURE,
                value = draft,
                isError = !valid && draft.isNotBlank(),
                onChange = onChange,
                imeAction = ImeAction.Done,
            )
        },
        confirmButton = { TextButton(onSave, enabled = valid, modifier = Modifier.heightIn(min = 48.dp)) { Text("적용") } },
        dismissButton = { TextButton(onDismiss, Modifier.heightIn(min = 48.dp)) { Text("취소") } },
    )
}

internal val EnergyStatus.title: String
    get() = when (this) {
        EnergyStatus.NORMAL -> "현재 입력 기준 유지 가능"
        EnergyStatus.AT_RISK -> "출발 전 부족 예상"
        EnergyStatus.INSUFFICIENT -> "현재 귀가용 전력 부족"
        EnergyStatus.MISSING_DATA -> "필요한 데이터 없음"
        EnergyStatus.INVALID_INPUT -> "계획 입력 확인 필요"
    }

internal fun EnergyEstimate.shortfallLabel(): String? = when (status) {
    EnergyStatus.AT_RISK -> "출발 시 부족 예상 ${departureShortfallKwh.displayAmount("kWh")}"
    EnergyStatus.INSUFFICIENT -> "현재 부족량 ${currentShortfallKwh.displayAmount("kWh")}"
    else -> null
}

internal fun Double?.displayAmount(unit: String): String {
    val number = this?.takeIf { it.isFinite() && it >= 0 } ?: return "—"
    val formatter = NumberFormat.getNumberInstance().apply { maximumFractionDigits = 2 }
    return "${formatter.format(number)}${if (unit == "%") "" else " "}$unit"
}

internal fun Double?.displayDuration(approximate: Boolean = true): String {
    val hours = this?.takeIf { it.isFinite() && it >= 0 } ?: return "—"
    val prefix = if (approximate && hours > 0) "약 " else ""
    if (hours >= Long.MAX_VALUE / 60.0) return prefix + hours.displayAmount("시간")
    val minutes = (hours * 60).roundToLong()
    val parts = buildList {
        if (minutes >= 60) add("${minutes / 60}시간")
        if (minutes % 60 != 0L || minutes < 60) add("${minutes % 60}분")
    }
    return prefix + parts.joinToString(" ")
}

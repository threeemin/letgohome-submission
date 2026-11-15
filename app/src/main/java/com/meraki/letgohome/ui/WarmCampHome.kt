package com.meraki.letgohome.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.meraki.letgohome.R
import com.meraki.letgohome.contract.BatteryConnection
import com.meraki.letgohome.contract.BatterySnapshot
import com.meraki.letgohome.contract.DataSource
import com.meraki.letgohome.contract.EnergyEstimate
import com.meraki.letgohome.contract.EnergyStatus

internal enum class CampPage(val label: String, val icon: CampIcon) {
    HOME("홈", CampIcon.HOME),
    POWER("전력", CampIcon.POWER),
    RETURN("귀가", CampIcon.PIN),
    SETTINGS("설정", CampIcon.SETTINGS),
}

@Composable
internal fun CampHeader(battery: BatterySnapshot) {
    val status = when {
        battery.connection == BatteryConnection.LOADING -> "배터리 확인 중"
        battery.source == DataSource.DEMO -> "시연 데이터"
        battery.source == DataSource.MANUAL -> "직접 입력"
        battery.connection == BatteryConnection.CONNECTED -> "차량 연결됨"
        else -> "차량 연결 미확인"
    }
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        CampGlyph(CampIcon.CAR, Modifier.size(30.dp))
        Text(
            stringResource(R.string.app_name),
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.weight(1f),
        )
        Surface(color = CampSand, contentColor = CampBlue, shape = RoundedCornerShape(50)) {
            Text(status, Modifier.padding(horizontal = 12.dp, vertical = 6.dp), style = MaterialTheme.typography.labelMedium)
        }
    }
}

@Composable
internal fun CampNavigation(selected: CampPage, onSelect: (CampPage) -> Unit) {
    val shape = RoundedCornerShape(14.dp)
    Surface(color = CampCard, shape = MaterialTheme.shapes.large) {
        BoxWithConstraints(Modifier.fillMaxWidth().padding(4.dp)) {
            val includeIcons = maxWidth >= 320.dp && LocalDensity.current.fontScale <= 1.3f
            Row(Modifier.selectableGroup(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                CampPage.entries.forEach { page ->
                    val active = page == selected
                    val foreground = if (active) CampCard else CampBlue
                    Surface(
                        modifier = Modifier.weight(1f).clip(shape).selectable(
                            selected = active,
                            onClick = { onSelect(page) },
                            role = Role.Tab,
                        ),
                        shape = shape,
                        color = if (active) CampBlue else CampCard,
                        contentColor = foreground,
                    ) {
                        Row(
                            modifier = Modifier.heightIn(min = 48.dp).padding(horizontal = 4.dp, vertical = 4.dp),
                            horizontalArrangement = Arrangement.spacedBy(6.dp, Alignment.CenterHorizontally),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            if (includeIcons) CampGlyph(page.icon, Modifier.size(18.dp), foreground)
                            Text(page.label, style = MaterialTheme.typography.labelLarge)
                        }
                    }
                }
            }
        }
    }
}

@Composable
internal fun WarmCampHome(
    battery: BatterySnapshot,
    estimate: EnergyEstimate,
    shortWindow: Boolean,
    onStart: () -> Unit,
    onRoute: () -> Unit,
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = CampSand,
        contentColor = CampBlue,
        shape = MaterialTheme.shapes.extraLarge,
    ) {
        BoxWithConstraints(Modifier.fillMaxWidth().padding(if (shortWindow) 10.dp else 16.dp)) {
            val wide = maxWidth >= 600.dp && LocalDensity.current.fontScale <= 1.3f
            Column(verticalArrangement = Arrangement.spacedBy(if (shortWindow) 6.dp else 12.dp)) {
                if (wide) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(24.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column(Modifier.weight(1.55f), verticalArrangement = Arrangement.spacedBy(if (shortWindow) 8.dp else 16.dp)) {
                            CampingTime(battery, estimate, wide = true, shortWindow = shortWindow)
                            CampActions(onStart, onRoute, shortWindow)
                        }
                        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            BatteryCard(battery, estimate, wide = !shortWindow)
                            CampScenery(Modifier.fillMaxWidth().height(if (shortWindow) 56.dp else 80.dp))
                        }
                    }
                    EnergyBreakdown(estimate, wide = true, shortWindow = shortWindow)
                } else {
                    CampingTime(battery, estimate, wide = false)
                    BatteryCard(battery, estimate, wide = false)
                    EnergyBreakdown(estimate, wide = false)
                    CampActions(onStart, onRoute)
                }
            }
        }
    }
}

@Composable
private fun CampingTime(
    battery: BatterySnapshot,
    estimate: EnergyEstimate,
    wide: Boolean,
    shortWindow: Boolean = false,
) {
    val guidance = when {
        battery.connection == BatteryConnection.LOADING && estimate.remainingHours == null -> "배터리 정보를 불러오는 중입니다."
        estimate.status == EnergyStatus.INVALID_INPUT -> "설정에서 귀가 계획을 확인해 주세요."
        estimate.remainingHours == null -> "배터리 정보와 귀가 계획을 확인해 주세요."
        else -> "현재 소비전력 기준"
    }
    Column(Modifier.padding(horizontal = 4.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text("예상 차박 가능 시간", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        Text(
            estimate.remainingHours.displayDuration(approximate = false),
            fontSize = if (wide && !shortWindow) 62.sp else if (wide) 50.sp else 44.sp,
            lineHeight = if (wide && !shortWindow) 68.sp else if (wide) 54.sp else 52.sp,
            fontWeight = FontWeight.ExtraBold,
        )
        if (estimate.status.isWarning) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                CampGlyph(CampIcon.WARNING, Modifier.size(18.dp), CampWarning)
                Text(estimate.status.title, color = CampWarning, style = MaterialTheme.typography.labelLarge)
            }
        } else if (!shortWindow || estimate.remainingHours == null) {
            Text(guidance, style = MaterialTheme.typography.bodyMedium)
        }
    }
}

@Composable
private fun BatteryCard(battery: BatterySnapshot, estimate: EnergyEstimate, wide: Boolean) {
    Surface(color = CampCard, shape = MaterialTheme.shapes.large, modifier = Modifier.fillMaxWidth()) {
        Row(
            Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            CampGlyph(CampIcon.BATTERY, Modifier.size(if (wide) 42.dp else 32.dp))
            Column(Modifier.weight(1f)) {
                Text("현재 배터리", style = MaterialTheme.typography.labelLarge)
                Text(
                    battery.socPercent?.takeIf { it in 0.0..100.0 }.displayAmount("%"),
                    style = if (wide) MaterialTheme.typography.displaySmall else MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                )
            }
            if (estimate.currentEnergyKwh != null) {
                Text(estimate.currentEnergyKwh.displayAmount("kWh"), style = MaterialTheme.typography.bodyMedium)
            }
        }
    }
}

@Composable
private fun EnergyBreakdown(estimate: EnergyEstimate, wide: Boolean, shortWindow: Boolean = false) {
    if (wide) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            EnergyMetric("귀가 예상 소모량", estimate.returnDriveEnergyKwh, CampIcon.HOME, Modifier.weight(1f), wide = true, shortWindow = shortWindow)
            EnergyMetric("추가 여유분", estimate.safetyEnergyKwh, CampIcon.SHIELD, Modifier.weight(1f), wide = true, shortWindow = shortWindow)
            EnergyMetric("차박 사용 가능량", estimate.campingEnergyKwh, CampIcon.TENT, Modifier.weight(1f), wide = true, shortWindow = shortWindow)
        }
    } else {
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            EnergyMetric("귀가 예상 소모량", estimate.returnDriveEnergyKwh, CampIcon.HOME)
            EnergyMetric("추가 여유분", estimate.safetyEnergyKwh, CampIcon.SHIELD)
            EnergyMetric("차박 사용 가능량", estimate.campingEnergyKwh, CampIcon.TENT)
        }
    }
}

@Composable
private fun EnergyMetric(
    label: String,
    value: Double?,
    icon: CampIcon,
    modifier: Modifier = Modifier,
    wide: Boolean = false,
    shortWindow: Boolean = false,
) {
    Surface(
        modifier = modifier,
        color = CampCard,
        shape = MaterialTheme.shapes.large,
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = if (shortWindow) 5.dp else 9.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Surface(color = CampCanvas, shape = RoundedCornerShape(10.dp)) {
                CampGlyph(icon, Modifier.padding(7.dp).size(20.dp))
            }
            if (wide) {
                Column {
                    Text(label, style = MaterialTheme.typography.labelLarge)
                    Text(
                        value.displayAmount("kWh"),
                        style = if (shortWindow) MaterialTheme.typography.titleLarge else MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                    )
                }
            } else {
                Text(label, Modifier.weight(1f), style = MaterialTheme.typography.labelLarge)
                Text(value.displayAmount("kWh"), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun CampActions(onStart: () -> Unit, onRoute: () -> Unit, shortWindow: Boolean = false) {
    BoxWithConstraints(Modifier.fillMaxWidth()) {
        val sideBySide = maxWidth >= 248.dp && LocalDensity.current.fontScale <= 1.3f
        val primary: @Composable (Modifier) -> Unit = { modifier ->
            Button(
                onClick = onStart,
                modifier = modifier.heightIn(min = if (shortWindow) 48.dp else 52.dp),
                shape = MaterialTheme.shapes.medium,
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
            ) {
                ActionLabel(CampIcon.TENT, "차박 시작", primary = true)
            }
        }
        val secondary: @Composable (Modifier) -> Unit = { modifier ->
            OutlinedButton(
                onClick = onRoute,
                modifier = modifier.heightIn(min = if (shortWindow) 48.dp else 52.dp),
                shape = MaterialTheme.shapes.medium,
                colors = ButtonDefaults.outlinedButtonColors(containerColor = CampCard),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                border = BorderStroke(1.dp, CampCard),
            ) {
                ActionLabel(CampIcon.PIN, "귀가 경로", primary = false)
            }
        }
        if (sideBySide) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                primary(Modifier.weight(1f))
                secondary(Modifier.weight(1f))
            }
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                primary(Modifier.fillMaxWidth())
                secondary(Modifier.fillMaxWidth())
            }
        }
    }
}

@Composable
private fun ActionLabel(icon: CampIcon, label: String, primary: Boolean) {
    Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
        CampGlyph(icon, Modifier.size(18.dp), if (primary) CampCard else CampBlue)
        Text(label, fontWeight = FontWeight.Bold)
    }
}

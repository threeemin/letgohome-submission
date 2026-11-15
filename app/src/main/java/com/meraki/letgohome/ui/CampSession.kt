package com.meraki.letgohome.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.BiasAlignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.meraki.letgohome.R
import com.meraki.letgohome.contract.BatterySnapshot
import com.meraki.letgohome.contract.DataSource
import com.meraki.letgohome.contract.EnergyEstimate

@Composable
internal fun CampSession(
    battery: BatterySnapshot,
    estimate: EnergyEstimate,
    sceneHeight: Dp,
    onReturnPlan: () -> Unit,
    onFinish: () -> Unit,
) {
    Box(
        Modifier.fillMaxWidth().heightIn(min = sceneHeight).clip(MaterialTheme.shapes.extraLarge)
            .background(Color(0xFF08182F)),
        contentAlignment = Alignment.Center,
    ) {
        Image(
            painterResource(R.drawable.camp_night),
            contentDescription = null,
            modifier = Modifier.matchParentSize(),
            contentScale = ContentScale.Crop,
            alignment = BiasAlignment(horizontalBias = 0.48f, verticalBias = 0f),
        )
        Box(
            Modifier.matchParentSize().background(
                Brush.verticalGradient(
                    0f to Color.Transparent,
                    0.54f to Color(0x72071022),
                    1f to Color(0x38071022),
                ),
            ),
        )
        Column(
            Modifier.padding(horizontal = 20.dp, vertical = 28.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                "남은 전력은 제가 안전하게 모니터링 할게요!",
                color = Color.White,
                style = MaterialTheme.typography.titleLarge.copy(fontSize = 18.sp, lineHeight = 26.sp),
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
            )
            Text(
                "편안한 시간 되세요:)",
                color = Color.White,
                style = MaterialTheme.typography.titleMedium,
                textAlign = TextAlign.Center,
            )
            Text("아래로 내려 전력 확인 ↓", color = Color(0xFFE6EBF4), style = MaterialTheme.typography.labelLarge)
        }
    }
    EnergyPanel(color = if (estimate.status.isWarning) CampWarningSurface else CampCard) {
        Text("현재 전력 상태", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        if (battery.source == DataSource.DEMO) Text("시연 배터리", style = MaterialTheme.typography.labelMedium)
        Text("현재 배터리 ${battery.socPercent.displayAmount("%")} · ${estimate.currentEnergyKwh.displayAmount("kWh")}")
        EnergySplit(estimate)
        Text(
            estimate.status.title,
            color = if (estimate.status.isWarning) CampWarning else CampBlue,
            style = MaterialTheme.typography.titleMedium,
        )
        estimate.shortfallLabel()?.let { Text(it, color = CampWarning) }
        TextButton(onClick = onReturnPlan) { Text("귀가 계획 보기") }
    }
    TextButton(onClick = onFinish) { Text("차박 종료") }
}

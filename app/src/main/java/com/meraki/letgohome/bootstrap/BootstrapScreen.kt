package com.meraki.letgohome.bootstrap

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.meraki.letgohome.contract.BatterySnapshot

/** Temporary startup screen. The product UI and calculator are separate team tasks. */
@Composable
fun BootstrapScreen(battery: BatterySnapshot) {
    MaterialTheme {
        Surface(modifier = Modifier.fillMaxSize(), color = Color(0xFFF5F4EE)) {
            Column(
                modifier = Modifier
                    .safeDrawingPadding()
                    .verticalScroll(rememberScrollState())
                    .padding(32.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                Text("MERAKI · LET GO HOME", color = Color(0xFF146252))
                Text("렛고홈", style = MaterialTheme.typography.headlineLarge)
                Text("Android 뼈대와 공통 타입이 준비되었습니다.")
                Text("시연 데이터 · 가상 차량")
                Text("배터리 SOC: ${battery.socPercent?.toString() ?: "—"}%")
                battery.message?.let { Text(it) }
                Text("다음 작업: 전력 계산 로직과 설정·대시보드·알림 화면 연결")
                Text("이 화면은 개발 시작용이며 실제 차량 데이터·전력 예측 기능은 아직 연결하지 않았습니다.")
            }
        }
    }
}

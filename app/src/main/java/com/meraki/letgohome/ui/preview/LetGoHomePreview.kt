package com.meraki.letgohome.ui.preview

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.meraki.letgohome.contract.BatteryConnection
import com.meraki.letgohome.contract.BatterySnapshot
import com.meraki.letgohome.contract.DataSource
import com.meraki.letgohome.contract.DemoDefaults
import com.meraki.letgohome.contract.EnergyEstimate
import com.meraki.letgohome.contract.EnergyInputs
import com.meraki.letgohome.contract.EnergyStatus
import com.meraki.letgohome.ui.LetGoHomeApp

@Preview(name = "차량 가로 화면", widthDp = 960, heightDp = 540)
@Preview(name = "좁은 화면", widthDp = 360, heightDp = 740)
@Composable
private fun LetGoHomePreview() {
    LetGoHomeApp(
        battery = BatterySnapshot(
            socPercent = DemoDefaults.baseline.socPercent,
            source = DataSource.DEMO,
            connection = BatteryConnection.UNAVAILABLE,
        ),
        calculate = ::previewEstimate,
    )
}

private fun previewEstimate(input: EnergyInputs): EnergyEstimate {
    val increasedPower = input.averagePowerKw == 2.5
    val atRisk = increasedPower && input.hoursUntilDeparture == 6.0
    return EnergyEstimate(
        status = if (atRisk) EnergyStatus.AT_RISK else EnergyStatus.NORMAL,
        currentEnergyKwh = 40.0,
        returnDriveEnergyKwh = 20.0,
        safetyEnergyKwh = 8.0,
        reservedEnergyKwh = 28.0,
        campingEnergyKwh = 12.0,
        remainingHours = if (increasedPower) 4.8 else 8.0,
        departureShortfallKwh = if (atRisk) 3.0 else 0.0,
    )
}

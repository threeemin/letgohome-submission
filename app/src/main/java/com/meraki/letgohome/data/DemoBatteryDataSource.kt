package com.meraki.letgohome.data

import com.meraki.letgohome.contract.BatteryConnection
import com.meraki.letgohome.contract.BatteryDataSource
import com.meraki.letgohome.contract.BatterySnapshot
import com.meraki.letgohome.contract.ChargingState
import com.meraki.letgohome.contract.DataSource
import com.meraki.letgohome.contract.DemoDefaults

/** Emits one explicitly labelled demo snapshot; no vehicle connection or background work. */
class DemoBatteryDataSource : BatteryDataSource {
    override fun start(onUpdate: (BatterySnapshot) -> Unit) {
        onUpdate(
            BatterySnapshot(
                socPercent = DemoDefaults.baseline.socPercent,
                source = DataSource.DEMO,
                connection = BatteryConnection.UNAVAILABLE,
                message = "차량 연동 없이 시연 데이터 사용",
                charging = ChargingState.NOT_CHARGING,
            ),
        )
    }

    override fun stop() = Unit
}

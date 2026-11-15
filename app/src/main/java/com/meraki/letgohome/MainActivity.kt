package com.meraki.letgohome

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.meraki.letgohome.contract.BatteryConnection
import com.meraki.letgohome.contract.BatteryDataSource
import com.meraki.letgohome.contract.BatterySnapshot
import com.meraki.letgohome.contract.DataSource
import com.meraki.letgohome.data.DemoBatteryDataSource
import com.meraki.letgohome.domain.EnergyCalculator
import com.meraki.letgohome.route.RouteSelection
import com.meraki.letgohome.ui.LetGoHomeApp

class MainActivity : ComponentActivity() {
    private val batterySource: BatteryDataSource = DemoBatteryDataSource()
    private var battery by mutableStateOf(
        BatterySnapshot(
            socPercent = null,
            source = DataSource.DEMO,
            connection = BatteryConnection.LOADING,
        ),
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val routeState = ViewModelProvider(this)[ReturnRouteViewModel::class.java]
        setContent {
            LetGoHomeApp(
                battery = battery,
                calculate = EnergyCalculator::calculate,
                selectedRoute = routeState.selection,
            )
        }
    }

    override fun onStart() {
        super.onStart()
        batterySource.start { snapshot ->
            runOnUiThread { battery = snapshot }
        }
    }

    override fun onStop() {
        batterySource.stop()
        super.onStop()
    }
}

internal class ReturnRouteViewModel : ViewModel() {
    val selection = mutableStateOf<RouteSelection?>(null)
}

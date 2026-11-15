package com.meraki.letgohome.contract

enum class DataSource { VEHICLE, MANUAL, DEMO }
enum class BatteryConnection { LOADING, CONNECTED, UNAVAILABLE, ERROR }
enum class ChargingState { CHARGING, NOT_CHARGING, UNKNOWN }

data class BatterySnapshot(
    val socPercent: Double?,
    val source: DataSource,
    val connection: BatteryConnection,
    val message: String? = null,
    val charging: ChargingState = ChargingState.UNKNOWN,
)

interface BatteryDataSource {
    fun start(onUpdate: (BatterySnapshot) -> Unit)
    fun stop()
}

data class EnergyInputs(
    val socPercent: Double?,
    val batteryCapacityKwh: Double?,
    val returnDistanceKm: Double?,
    val efficiencyKmPerKwh: Double?,
    val safetyReservePercentPoint: Double?,
    val averagePowerKw: Double?,
    val hoursUntilDeparture: Double?,
)

enum class EnergyStatus {
    NORMAL, AT_RISK, INSUFFICIENT, MISSING_DATA, INVALID_INPUT
}

data class EnergyEstimate(
    val status: EnergyStatus,
    val currentEnergyKwh: Double? = null,
    val returnDriveEnergyKwh: Double? = null,
    val safetyEnergyKwh: Double? = null,
    val reservedEnergyKwh: Double? = null,
    val campingEnergyKwh: Double? = null,
    val remainingHours: Double? = null,
    val currentShortfallKwh: Double? = null,
    val departureShortfallKwh: Double? = null,
    val invalidFields: List<String> = emptyList(),
)

object DemoDefaults {
    val baseline = EnergyInputs(
        socPercent = 50.0,
        batteryCapacityKwh = 80.0,
        returnDistanceKm = 100.0,
        efficiencyKmPerKwh = 5.0,
        safetyReservePercentPoint = 10.0,
        averagePowerKw = 1.5,
        hoursUntilDeparture = 6.0,
    )
}

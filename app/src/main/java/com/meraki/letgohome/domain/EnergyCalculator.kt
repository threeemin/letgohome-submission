package com.meraki.letgohome.domain

import com.meraki.letgohome.contract.EnergyEstimate
import com.meraki.letgohome.contract.EnergyInputs
import com.meraki.letgohome.contract.EnergyStatus

/** Estimates energy at the current, constant power draw; keeps display rounding in the UI. */
object EnergyCalculator {
    private const val SHORTFALL_TOLERANCE_KWH = 1e-9

    fun calculate(input: EnergyInputs): EnergyEstimate {
        val invalidFields = mutableListOf<String>()
        val missingFields = mutableListOf<String>()

        fun validate(name: String, value: Double?, inRange: (Double) -> Boolean) {
            when {
                value == null -> missingFields.add(name)
                !value.isFinite() || !inRange(value) -> invalidFields.add(name)
            }
        }

        validate("socPercent", input.socPercent) { it in 0.0..100.0 }
        validate("batteryCapacityKwh", input.batteryCapacityKwh) { it > 0.0 }
        validate("returnDistanceKm", input.returnDistanceKm) { it >= 0.0 }
        validate("efficiencyKmPerKwh", input.efficiencyKmPerKwh) { it > 0.0 }
        validate("safetyReservePercentPoint", input.safetyReservePercentPoint) { it in 0.0..100.0 }
        validate("averagePowerKw", input.averagePowerKw) { it > 0.0 }
        validate("hoursUntilDeparture", input.hoursUntilDeparture) { it >= 0.0 }

        // Inspect every supplied value before deciding whether missing data is the problem.
        if (invalidFields.isNotEmpty()) {
            return EnergyEstimate(EnergyStatus.INVALID_INPUT, invalidFields = invalidFields)
        }
        if (missingFields.isNotEmpty()) {
            return EnergyEstimate(EnergyStatus.MISSING_DATA, invalidFields = missingFields)
        }

        val soc = requireNotNull(input.socPercent)
        val capacity = requireNotNull(input.batteryCapacityKwh)
        val distance = requireNotNull(input.returnDistanceKm)
        val efficiency = requireNotNull(input.efficiencyKmPerKwh)
        val reservePercent = requireNotNull(input.safetyReservePercentPoint)
        val power = requireNotNull(input.averagePowerKw)
        val departureHours = requireNotNull(input.hoursUntilDeparture)

        // For arithmetic overflow, report the input fields involved, never new field names.
        val overflowFields = linkedSetOf<String>()
        fun checked(value: Double, vararg fields: String): Double {
            if (!value.isFinite()) overflowFields.addAll(fields)
            return value
        }
        fun overflowResult() = EnergyEstimate(
            EnergyStatus.INVALID_INPUT,
            invalidFields = overflowFields.toList(),
        )

        // Apply the percentage first to avoid overflowing an otherwise representable value.
        val current = checked(capacity * (soc / 100.0), "batteryCapacityKwh", "socPercent")
        val drive = checked(distance / efficiency, "returnDistanceKm", "efficiencyKmPerKwh")
        val safety = checked(
            capacity * (reservePercent / 100.0),
            "batteryCapacityKwh", "safetyReservePercentPoint",
        )
        val departureConsumption = checked(
            power * departureHours, "averagePowerKw", "hoursUntilDeparture",
        )
        if (overflowFields.isNotEmpty()) return overflowResult()

        val reserved = checked(
            drive + safety,
            "returnDistanceKm", "efficiencyKmPerKwh",
            "batteryCapacityKwh", "safetyReservePercentPoint",
        )
        if (overflowFields.isNotEmpty()) return overflowResult()

        // Finite, nonnegative current/reserved/consumption values make these differences finite.
        val camping = maxOf(0.0, current - reserved)
        val currentShortfall = maxOf(0.0, reserved - current)
        val remaining = checked(
            camping / power,
            "socPercent", "batteryCapacityKwh", "returnDistanceKm",
            "efficiencyKmPerKwh", "safetyReservePercentPoint", "averagePowerKw",
        )
        val energyAtDeparture = current - departureConsumption
        // Keep the full projected deficit even if consumption exceeds the current battery.
        val departureShortfall = checked(
            maxOf(0.0, reserved - energyAtDeparture),
            "socPercent", "batteryCapacityKwh", "returnDistanceKm", "efficiencyKmPerKwh",
            "safetyReservePercentPoint", "averagePowerKw", "hoursUntilDeparture",
        )
        if (overflowFields.isNotEmpty()) return overflowResult()

        val status = when {
            currentShortfall > SHORTFALL_TOLERANCE_KWH -> EnergyStatus.INSUFFICIENT
            departureShortfall > SHORTFALL_TOLERANCE_KWH -> EnergyStatus.AT_RISK
            else -> EnergyStatus.NORMAL
        }
        return EnergyEstimate(
            status = status,
            currentEnergyKwh = current,
            returnDriveEnergyKwh = drive,
            safetyEnergyKwh = safety,
            reservedEnergyKwh = reserved,
            campingEnergyKwh = camping,
            remainingHours = remaining,
            currentShortfallKwh = currentShortfall,
            departureShortfallKwh = departureShortfall,
        )
    }
}

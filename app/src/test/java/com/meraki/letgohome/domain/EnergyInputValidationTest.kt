package com.meraki.letgohome.domain

import com.meraki.letgohome.contract.DemoDefaults
import com.meraki.letgohome.contract.EnergyEstimate
import com.meraki.letgohome.contract.EnergyInputs
import com.meraki.letgohome.contract.EnergyStatus
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

@RunWith(Parameterized::class)
class EnergyInputValidationTest(
    private val field: String,
    private val value: Double?,
    private val input: EnergyInputs,
) {
    @Test
    fun reportsTheAffectedFieldAndLeavesEveryNumericResultNull() {
        val status = if (value == null) EnergyStatus.MISSING_DATA else EnergyStatus.INVALID_INPUT
        assertEquals(
            EnergyEstimate(status, invalidFields = listOf(field)),
            EnergyCalculator.calculate(input),
        )
    }

    companion object {
        @JvmStatic
        @Parameterized.Parameters(name = "{index}: {0}={1}")
        fun cases(): Collection<Array<Any?>> {
            val baseline = DemoDefaults.baseline
            val cases = mutableListOf<Array<Any?>>()
            fun field(name: String, outOfRange: List<Double>, update: (Double?) -> EnergyInputs) {
                val values = listOf(null, Double.NaN, Double.POSITIVE_INFINITY, Double.NEGATIVE_INFINITY) + outOfRange
                values.forEach { cases.add(arrayOf(name, it, update(it))) }
            }
            field("socPercent", listOf(-1.0, 100.1)) { baseline.copy(socPercent = it) }
            field("batteryCapacityKwh", listOf(-1.0, 0.0, -0.0)) { baseline.copy(batteryCapacityKwh = it) }
            field("returnDistanceKm", listOf(-1.0)) { baseline.copy(returnDistanceKm = it) }
            field("efficiencyKmPerKwh", listOf(-1.0, 0.0, -0.0)) { baseline.copy(efficiencyKmPerKwh = it) }
            field("safetyReservePercentPoint", listOf(-1.0, 100.1)) { baseline.copy(safetyReservePercentPoint = it) }
            field("averagePowerKw", listOf(-1.0, 0.0, -0.0)) { baseline.copy(averagePowerKw = it) }
            field("hoursUntilDeparture", listOf(-1.0)) { baseline.copy(hoursUntilDeparture = it) }
            return cases
        }
    }
}

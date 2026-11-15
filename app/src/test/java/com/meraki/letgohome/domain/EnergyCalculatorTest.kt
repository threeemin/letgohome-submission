package com.meraki.letgohome.domain

import com.meraki.letgohome.contract.DemoDefaults
import com.meraki.letgohome.contract.EnergyEstimate
import com.meraki.letgohome.contract.EnergyInputs
import com.meraki.letgohome.contract.EnergyStatus
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class EnergyCalculatorTest {
    private val baseline = DemoDefaults.baseline

    @Test
    fun baselineReservesDriveEnergyAndTenPercentagePointsOfCapacity() {
        assertEstimate(
            baseline, EnergyStatus.NORMAL,
            current = 40.0, drive = 20.0, safety = 8.0, reserved = 28.0,
            camping = 12.0, remaining = 8.0, currentShortfall = 0.0, departureShortfall = 0.0,
        )
    }

    @Test
    fun increasedConsumptionGivesFourHoursFortyEightMinutesAndThreeKwhShortfall() {
        val result = assertEstimate(
            baseline.copy(averagePowerKw = 2.5), EnergyStatus.AT_RISK,
            current = 40.0, drive = 20.0, safety = 8.0, reserved = 28.0,
            camping = 12.0, remaining = 4.8, currentShortfall = 0.0, departureShortfall = 3.0,
        )
        assertEquals(288.0, requireNotNull(result.remainingHours) * 60.0, 1e-9)
    }

    @Test
    fun movingDepartureToFourHoursClearsThePreviousRisk() {
        val highPower = baseline.copy(averagePowerKw = 2.5)
        assertEquals(EnergyStatus.AT_RISK, EnergyCalculator.calculate(highPower).status)
        assertEstimate(
            highPower.copy(hoursUntilDeparture = 4.0), EnergyStatus.NORMAL,
            current = 40.0, drive = 20.0, safety = 8.0, reserved = 28.0,
            camping = 12.0, remaining = 4.8, currentShortfall = 0.0, departureShortfall = 0.0,
        )
        assertEquals(EnergyStatus.AT_RISK, EnergyCalculator.calculate(highPower).status)
    }

    @Test
    fun currentShortfallTakesPriorityOverProjectedShortfall() {
        assertEstimate(
            baseline.copy(socPercent = 30.0), EnergyStatus.INSUFFICIENT,
            current = 24.0, drive = 20.0, safety = 8.0, reserved = 28.0,
            camping = 0.0, remaining = 0.0, currentShortfall = 4.0, departureShortfall = 13.0,
        )
    }

    @Test
    fun exactlyAtCurrentReserveIsAtRiskWhenDepartureIsLater() {
        assertEstimate(
            baseline.copy(socPercent = 35.0), EnergyStatus.AT_RISK,
            current = 28.0, drive = 20.0, safety = 8.0, reserved = 28.0,
            camping = 0.0, remaining = 0.0, currentShortfall = 0.0, departureShortfall = 9.0,
        )
    }

    @Test
    fun exactlyAtDepartureReserveIsNormal() {
        assertEstimate(
            baseline.copy(hoursUntilDeparture = 8.0), EnergyStatus.NORMAL,
            current = 40.0, drive = 20.0, safety = 8.0, reserved = 28.0,
            camping = 12.0, remaining = 8.0, currentShortfall = 0.0, departureShortfall = 0.0,
        )
    }

    @Test
    fun departingNowAtTheCurrentReserveIsNormal() {
        assertEstimate(
            baseline.copy(socPercent = 35.0, hoursUntilDeparture = 0.0), EnergyStatus.NORMAL,
            current = 28.0, drive = 20.0, safety = 8.0, reserved = 28.0,
            camping = 0.0, remaining = 0.0, currentShortfall = 0.0, departureShortfall = 0.0,
        )
    }

    @Test
    fun validZeroBoundsDoNotCountAsMissingData() {
        assertEstimate(
            baseline.copy(
                socPercent = 0.0, returnDistanceKm = 0.0,
                safetyReservePercentPoint = 0.0, hoursUntilDeparture = 0.0,
            ), EnergyStatus.NORMAL,
            current = 0.0, drive = 0.0, safety = 0.0, reserved = 0.0,
            camping = 0.0, remaining = 0.0, currentShortfall = 0.0, departureShortfall = 0.0,
        )
    }

    @Test
    fun hundredPercentSocAndReserveAreValid() {
        assertEstimate(
            baseline.copy(
                socPercent = 100.0, returnDistanceKm = 0.0,
                safetyReservePercentPoint = 100.0, hoursUntilDeparture = 0.0,
            ), EnergyStatus.NORMAL,
            current = 80.0, drive = 0.0, safety = 80.0, reserved = 80.0,
            camping = 0.0, remaining = 0.0, currentShortfall = 0.0, departureShortfall = 0.0,
        )
    }

    @Test
    fun reserveAboveBatteryCapacityIsARealShortfall() {
        assertEstimate(
            baseline.copy(returnDistanceKm = 500.0), EnergyStatus.INSUFFICIENT,
            current = 40.0, drive = 100.0, safety = 8.0, reserved = 108.0,
            camping = 0.0, remaining = 0.0, currentShortfall = 68.0, departureShortfall = 77.0,
        )
    }

    @Test
    fun projectedConsumptionBeyondBatteryKeepsTheFullEnergyDeficit() {
        assertEstimate(
            baseline.copy(averagePowerKw = 10.0), EnergyStatus.AT_RISK,
            current = 40.0, drive = 20.0, safety = 8.0, reserved = 28.0,
            camping = 12.0, remaining = 1.2, currentShortfall = 0.0, departureShortfall = 48.0,
        )
    }

    @Test
    fun differentInputsRecalculateEveryBudgetWithoutDisplayRounding() {
        assertEstimate(
            EnergyInputs(62.5, 64.0, 73.0, 4.0, 12.5, 3.0, 5.0), EnergyStatus.AT_RISK,
            current = 40.0, drive = 18.25, safety = 8.0, reserved = 26.25,
            camping = 13.75, remaining = 13.75 / 3.0,
            currentShortfall = 0.0, departureShortfall = 1.25,
        )
    }

    @Test
    fun currentShortfallUsesStrictGreaterThanToleranceWithoutRoundingNumbers() {
        val tolerance = 1e-9
        for (shortfall in listOf(Math.nextDown(tolerance), tolerance, Math.nextUp(tolerance))) {
            val result = EnergyCalculator.calculate(EnergyInputs(0.0, 1.0, shortfall, 1.0, 0.0, 1.0, 0.0))
            val expected = if (shortfall > tolerance) EnergyStatus.INSUFFICIENT else EnergyStatus.NORMAL
            assertEquals("shortfall=$shortfall", expected, result.status)
            assertEquals(shortfall, requireNotNull(result.currentShortfallKwh), 0.0)
            assertEquals(0.0, requireNotNull(result.remainingHours), 0.0)
        }
    }

    @Test
    fun departureShortfallUsesStrictGreaterThanToleranceWithoutRoundingNumbers() {
        val tolerance = 1e-9
        for (shortfall in listOf(Math.nextDown(tolerance), tolerance, Math.nextUp(tolerance))) {
            val result = EnergyCalculator.calculate(EnergyInputs(0.0, 1.0, 0.0, 1.0, 0.0, shortfall, 1.0))
            val expected = if (shortfall > tolerance) EnergyStatus.AT_RISK else EnergyStatus.NORMAL
            assertEquals("shortfall=$shortfall", expected, result.status)
            assertEquals(shortfall, requireNotNull(result.departureShortfallKwh), 0.0)
            assertEquals(0.0, requireNotNull(result.currentShortfallKwh), 0.0)
        }
    }

    @Test
    fun allMissingInputsReportTheirNamesAndNoNumericResults() {
        assertEquals(
            EnergyEstimate(
                EnergyStatus.MISSING_DATA,
                invalidFields = listOf(
                    "socPercent", "batteryCapacityKwh", "returnDistanceKm", "efficiencyKmPerKwh",
                    "safetyReservePercentPoint", "averagePowerKw", "hoursUntilDeparture",
                ),
            ),
            EnergyCalculator.calculate(EnergyInputs(null, null, null, null, null, null, null)),
        )
    }

    @Test
    fun invalidInputTakesPriorityOverMissingSocAndClearsAllNumbers() {
        EnergyCalculator.calculate(baseline)
        assertEquals(
            EnergyEstimate(EnergyStatus.INVALID_INPUT, invalidFields = listOf("efficiencyKmPerKwh")),
            EnergyCalculator.calculate(baseline.copy(socPercent = null, efficiencyKmPerKwh = 0.0)),
        )
    }

    @Test
    fun allInvalidFieldsAreReportedEvenWithMissingData() {
        assertEquals(
            EnergyEstimate(
                EnergyStatus.INVALID_INPUT,
                invalidFields = listOf("batteryCapacityKwh", "safetyReservePercentPoint", "averagePowerKw"),
            ),
            EnergyCalculator.calculate(
                baseline.copy(
                    socPercent = null, batteryCapacityKwh = -1.0,
                    safetyReservePercentPoint = Double.NaN, averagePowerKw = 0.0,
                ),
            ),
        )
    }

    @Test
    fun overflowingDriveEnergyIsInvalid() {
        assertOverflow(
            baseline.copy(returnDistanceKm = Double.MAX_VALUE, efficiencyKmPerKwh = Double.MIN_VALUE),
            "returnDistanceKm", "efficiencyKmPerKwh",
        )
    }

    @Test
    fun overflowingDepartureConsumptionIsInvalidEvenIfOtherNumbersAreFinite() {
        assertOverflow(
            baseline.copy(averagePowerKw = Double.MAX_VALUE, hoursUntilDeparture = 2.0),
            "averagePowerKw", "hoursUntilDeparture",
        )
    }

    @Test
    fun overflowingReserveSumIsInvalidBeforeCampingIsClampedToZero() {
        assertOverflow(
            EnergyInputs(0.0, Double.MAX_VALUE, Double.MAX_VALUE, 1.0, 100.0, 1.0, 0.0),
            "returnDistanceKm", "efficiencyKmPerKwh", "batteryCapacityKwh", "safetyReservePercentPoint",
        )
    }

    @Test
    fun overflowingRemainingTimeIsInvalid() {
        assertOverflow(baseline.copy(averagePowerKw = Double.MIN_VALUE), "averagePowerKw")
    }

    @Test
    fun overflowingDepartureDeficitIsInvalidEvenWhenConsumptionAndReserveAreFinite() {
        assertOverflow(
            EnergyInputs(0.0, Double.MAX_VALUE, 0.0, 1.0, 100.0, Double.MAX_VALUE, 1.0),
            "batteryCapacityKwh", "safetyReservePercentPoint", "averagePowerKw", "hoursUntilDeparture",
        )
    }

    @Test
    fun largeRepresentablePercentageCalculationRemainsValid() {
        assertEstimate(
            EnergyInputs(100.0, Double.MAX_VALUE, 0.0, 1.0, 100.0, 1.0, 0.0), EnergyStatus.NORMAL,
            current = Double.MAX_VALUE, drive = 0.0, safety = Double.MAX_VALUE, reserved = Double.MAX_VALUE,
            camping = 0.0, remaining = 0.0, currentShortfall = 0.0, departureShortfall = 0.0,
        )
    }

    private fun assertOverflow(input: EnergyInputs, vararg involvedFields: String) {
        val result = EnergyCalculator.calculate(input)
        assertEquals(EnergyEstimate(EnergyStatus.INVALID_INPUT, invalidFields = result.invalidFields), result)
        assertTrue(result.invalidFields.containsAll(involvedFields.toList()))
        assertEquals(result.invalidFields.distinct(), result.invalidFields)
        assertTrue(result.invalidFields.all { it in inputFieldNames })
    }

    private fun assertEstimate(
        input: EnergyInputs,
        status: EnergyStatus,
        current: Double,
        drive: Double,
        safety: Double,
        reserved: Double,
        camping: Double,
        remaining: Double,
        currentShortfall: Double,
        departureShortfall: Double,
    ): EnergyEstimate {
        val result = EnergyCalculator.calculate(input)
        assertEquals(status, result.status)
        assertTrue(result.invalidFields.isEmpty())
        val expected = listOf(current, drive, safety, reserved, camping, remaining, currentShortfall, departureShortfall)
        val actual = listOf(
            result.currentEnergyKwh, result.returnDriveEnergyKwh, result.safetyEnergyKwh,
            result.reservedEnergyKwh, result.campingEnergyKwh, result.remainingHours,
            result.currentShortfallKwh, result.departureShortfallKwh,
        )
        expected.zip(actual).forEachIndexed { index, (wanted, number) ->
            val value = requireNotNull(number) { "Missing result at index $index" }
            assertTrue("Non-finite or negative result at index $index", value.isFinite() && value >= 0.0)
            assertEquals("Result at index $index", wanted, value, 1e-9)
        }
        return result
    }

    private val inputFieldNames = setOf(
        "socPercent", "batteryCapacityKwh", "returnDistanceKm", "efficiencyKmPerKwh",
        "safetyReservePercentPoint", "averagePowerKw", "hoursUntilDeparture",
    )
}

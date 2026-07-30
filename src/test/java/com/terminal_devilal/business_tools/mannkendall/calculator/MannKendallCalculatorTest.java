package com.terminal_devilal.business_tools.mannkendall.calculator;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.Test;

class MannKendallCalculatorTest {

    private final MannKendallCalculator calculator = new MannKendallCalculatorImpl();

    @Test
    void shouldCalculateIncreasingSeries() {
        MannKendallCalcResult result = calculator.calculate(List.of(1.0d, 2.0d, 3.0d, 4.0d, 5.0d));

        assertEquals("increasing", result.getTrend());
        assertTrue(result.isH());
        assertEquals(10L, result.getS());
        assertEquals(1.0d, result.getTau(), 1.0e-9d);
        assertEquals(16.666666666666668d, result.getVarS(), 1.0e-9d);
        assertEquals(1.0d, result.getSlope(), 1.0e-9d);
        assertEquals(1.0d, result.getIntercept(), 1.0e-9d);
        assertTrue(result.getP() < 0.05d);
    }

    @Test
    void shouldCalculateDecreasingSeries() {
        MannKendallCalcResult result = calculator.calculate(List.of(5.0d, 4.0d, 3.0d, 2.0d, 1.0d));

        assertEquals("decreasing", result.getTrend());
        assertTrue(result.isH());
        assertEquals(-10L, result.getS());
        assertEquals(-1.0d, result.getTau(), 1.0e-9d);
        assertEquals(-1.0d, result.getSlope(), 1.0e-9d);
        assertEquals(5.0d, result.getIntercept(), 1.0e-9d);
        assertTrue(result.getP() < 0.05d);
    }

    @Test
    void shouldCalculateFlatSeries() {
        MannKendallCalcResult result = calculator.calculate(List.of(2.0d, 2.0d, 2.0d, 2.0d));

        assertEquals("no trend", result.getTrend());
        assertFalse(result.isH());
        assertEquals(0L, result.getS());
        assertEquals(0.0d, result.getTau(), 1.0e-9d);
        assertEquals(0.0d, result.getVarS(), 1.0e-9d);
        assertEquals(0.0d, result.getZ(), 1.0e-9d);
        assertEquals(1.0d, result.getP(), 1.0e-9d);
        assertEquals(0.0d, result.getSlope(), 1.0e-9d);
        assertEquals(2.0d, result.getIntercept(), 1.0e-9d);
    }

    @Test
    void shouldHandleSeriesWithTies() {
        MannKendallCalcResult result = calculator.calculate(Arrays.asList(100.0d, 100.0d, 100.0d, 105.0d, 110.0d, 110.0d));

        assertEquals(11L, result.getS());
        assertEquals(23.666666666666668d, result.getVarS(), 1.0e-9d);
        assertEquals(0.7333333333333333d, result.getTau(), 1.0e-9d);
        assertTrue(result.isH());
        assertEquals("increasing", result.getTrend());
    }

    @Test
    void shouldHandleSmallestValidDataset() {
        MannKendallCalcResult result = calculator.calculate(List.of(1.0d, 2.0d, 3.0d));

        assertEquals("no trend", result.getTrend());
        assertFalse(result.isH());
        assertEquals(3L, result.getS());
        assertEquals(1.0d, result.getTau(), 1.0e-9d);
    }

    @Test
    void shouldHandleLargeDataset() {
        Double[] values = new Double[200];
        for (int index = 0; index < values.length; index++) {
            values[index] = (double) index;
        }

        MannKendallCalcResult result = calculator.calculate(Arrays.asList(values));

        assertEquals("increasing", result.getTrend());
        assertTrue(result.isH());
        assertEquals(1.0d, result.getSlope(), 1.0e-9d);
        assertEquals(1.0d, result.getTau(), 1.0e-9d);
    }

    @Test
    void shouldTreatNoisyNonSignificantSeriesAsNoTrend() {
        MannKendallCalcResult result = calculator.calculate(List.of(1.0d, 2.0d, 1.0d, 2.0d, 1.0d));

        assertFalse(result.isH());
        assertEquals("no trend", result.getTrend());
        assertTrue(result.getP() >= 0.05d);
    }

    @Test
    void shouldRejectNullInput() {
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> calculator.calculate(null));

        assertTrue(exception.getMessage().contains("cannot be null"));
    }

    @Test
    void shouldRejectEmptyInput() {
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> calculator.calculate(List.of()));

        assertTrue(exception.getMessage().contains("at least 3 observations"));
    }

    @Test
    void shouldRejectInputsWithFewerThanThreeObservations() {
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> calculator.calculate(List.of(1.0d, 2.0d)));

        assertTrue(exception.getMessage().contains("at least 3 observations"));
    }

    @Test
    void shouldRejectInvalidNumericValues() {
        IllegalArgumentException nullValueException = assertThrows(IllegalArgumentException.class,
                () -> calculator.calculate(Arrays.asList(1.0d, null, 3.0d)));
        assertTrue(nullValueException.getMessage().contains("invalid numeric value"));

        IllegalArgumentException nanValueException = assertThrows(IllegalArgumentException.class,
                () -> calculator.calculate(Arrays.asList(1.0d, Double.NaN, 3.0d)));
        assertTrue(nanValueException.getMessage().contains("invalid numeric value"));

        IllegalArgumentException infiniteValueException = assertThrows(IllegalArgumentException.class,
                () -> calculator.calculate(Arrays.asList(1.0d, Double.POSITIVE_INFINITY, 3.0d)));
        assertTrue(infiniteValueException.getMessage().contains("invalid numeric value"));
    }
}
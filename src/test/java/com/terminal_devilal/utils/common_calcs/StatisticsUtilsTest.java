package com.terminal_devilal.utils.common_calcs;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class StatisticsUtilsTest {

    @Test
    void shouldComputeMedianForEvenSizedSortedArray() {
        assertEquals(2.5d, StatisticsUtils.medianSorted(new double[] { 1.0d, 2.0d, 3.0d, 4.0d }), 1.0e-9d);
    }

    @Test
    void shouldComputeTieAdjustedVariance() {
        double variance = StatisticsUtils.computeVarianceS(new double[] { 100.0d, 100.0d, 100.0d, 105.0d, 110.0d, 110.0d });

        assertEquals(23.666666666666668d, variance, 1.0e-9d);
    }

    @Test
    void shouldComputeMannKendallStatistics() {
        double[] values = new double[] { 1.0d, 2.0d, 3.0d, 4.0d, 5.0d };

        assertEquals(10L, StatisticsUtils.computeS(values));
        assertEquals(1.0d, StatisticsUtils.computeTau(10L, values.length), 1.0e-9d);
        assertTrue(StatisticsUtils.computeZ(10L, 16.666666666666668d) > 0.0d);
        assertEquals(1.0d, StatisticsUtils.computeIntercept(3.0d, 1.0d, values.length), 1.0e-9d);
    }

    @Test
    void shouldComputeSenSlopeAndPValue() {
        double[] values = new double[] { 1.0d, 2.0d, 3.0d, 4.0d, 5.0d };

        assertEquals(1.0d, StatisticsUtils.computeSenSlope(values), 1.0e-9d);
        assertEquals(1.0d, StatisticsUtils.computePValue(0.0d), 1.0e-9d);
        assertTrue(StatisticsUtils.computePValue(2.0d) < 0.05d);
        assertEquals("increasing", StatisticsUtils.determineTrend(true, 0.5d));
        assertEquals("decreasing", StatisticsUtils.determineTrend(true, -0.5d));
        assertEquals("no trend", StatisticsUtils.determineTrend(false, 0.5d));
        assertEquals("no trend", StatisticsUtils.determineTrend(true, 0.0d));
    }
}
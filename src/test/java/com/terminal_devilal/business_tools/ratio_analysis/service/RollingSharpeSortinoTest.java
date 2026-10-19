package com.terminal_devilal.business_tools.ratio_analysis.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class RollingSharpeSortinoTest {

    @Test
    void rollingRatiosUseConfiguredNumberOfReturns() {
        int window = 4;
        double riskFreeRate = 0.06;
        double[] returns = { 0.02, -0.03, 0.01, -0.01 };

        RollingSharpeSortino rolling = new RollingSharpeSortino(window, riskFreeRate);
        for (double observation : returns) {
            rolling.add(observation);
        }

        double mar = riskFreeRate / 252.0;
        double mean = 0.0;
        for (double observation : returns) {
            mean += observation;
        }
        mean /= window;

        double variance = 0.0;
        double downsideVariance = 0.0;
        for (double observation : returns) {
            variance += (observation - mean) * (observation - mean);
            double downside = Math.min(observation - mar, 0.0);
            downsideVariance += downside * downside;
        }
        variance /= window;
        downsideVariance /= window;

        assertTrue(rolling.isReady());
        assertEquals((mean - mar) / Math.sqrt(variance), rolling.getSharpe(), 1e-12);
        assertEquals((mean - mar) / Math.sqrt(downsideVariance), rolling.getSortino(), 1e-12);
    }

    @Test
    void pooledCalculatorIsResetAndReusedAfterRelease() {
        RollingSharpeSortinoPool pool = new RollingSharpeSortinoPool(1);
        RollingSharpeSortino firstInstance;
        try (RollingSharpeSortinoPool.Lease lease = pool.acquire(2, 0.06)) {
            firstInstance = lease.getRolling();
            firstInstance.add(0.01);
            firstInstance.add(-0.02);
            assertTrue(firstInstance.isReady());
        }

        try (RollingSharpeSortinoPool.Lease lease = pool.acquire(2, 0.06)) {
            RollingSharpeSortino reusedInstance = lease.getRolling();
            assertEquals(firstInstance, reusedInstance);
            assertFalse(reusedInstance.isReady());
            reusedInstance.add(0.03);
            reusedInstance.add(0.02);
            assertTrue(reusedInstance.isReady());
        }
    }
}

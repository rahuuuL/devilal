package com.terminal_devilal.business_tools.pvpp.calculator;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import org.junit.jupiter.api.Test;

import com.terminal_devilal.indicators.pdv.entity.PriceDeliveryVolumeEntity;

class PvppCalculatorImplTest {

    private final PvppCalculator calculator = new PvppCalculatorImpl();

    @Test
    void shouldCalculateBodyWeight() {
        PriceDeliveryVolumeEntity candle = new PriceDeliveryVolumeEntity();
        candle.setOpen(105.0d);
        candle.setClose(100.0d);
        candle.setHigh(110.0d);
        candle.setLow(90.0d);

        assertEquals(0.25d, calculator.calculateBodyWeight(candle), 1.0e-9d);
    }

    @Test
    void shouldReturnNullForBodyWeightWhenRangeIsZero() {
        PriceDeliveryVolumeEntity candle = new PriceDeliveryVolumeEntity();
        candle.setOpen(100.0d);
        candle.setClose(100.0d);
        candle.setHigh(100.0d);
        candle.setLow(100.0d);

        assertNull(calculator.calculateBodyWeight(candle));
    }
}

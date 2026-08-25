package com.terminal_devilal.business_tools.pvpp.calculator;

import java.time.LocalDate;
import java.util.List;

import com.terminal_devilal.indicators.pdv.entity.PriceDeliveryVolumeEntity;

public interface PvppCalculator {

    PvppCalcResult computeAllWindows(String ticker, List<PriceDeliveryVolumeEntity> data, List<Integer> enabledDays);

    // Same rolling computation as computeAllWindows but only emits rows for targetDate, avoiding re-emitting the whole lookback window
    PvppCalcResult computeForDate(String ticker, List<PriceDeliveryVolumeEntity> data, List<Integer> enabledDays,
            LocalDate targetDate);

    PvppCalcResult.PvppRow calculateDailyRow(PriceDeliveryVolumeEntity current);

    Double calculateReturnPct(PriceDeliveryVolumeEntity current);

    Double calculateClv(PriceDeliveryVolumeEntity current);

    Double calculateCenteredClv(PriceDeliveryVolumeEntity current);

    Double calculateRvol(List<PriceDeliveryVolumeEntity> data, int days, int index);

    Double calculateEfficiency(Double returnPct, Double rvol);
}

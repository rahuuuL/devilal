package com.terminal_devilal.indicators.rsi.entities.projections;

import java.time.LocalDate;

public interface RsiPercentileProjection {
    String getTicker();

    LocalDate getDate();

    double getFourtheenDaysRSI();

    double getTweentyOneDaysRSI();
}

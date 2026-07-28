package com.terminal_devilal.business_tools.mannkendall.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalDate;

import org.junit.jupiter.api.Test;

import com.terminal_devilal.utils.WorkingDayDateRangeUtil;

class WorkingDayDateRangeUtilTest {

    @Test
    void shouldCalculateFromDateForFiveWorkingDays() {
        LocalDate toDate = LocalDate.of(2026, 7, 26);

        WorkingDayDateRangeUtil.DateRange range = WorkingDayDateRangeUtil.calculateDateRange(toDate, 5);

        assertEquals(LocalDate.of(2026, 7, 19), range.getFromDate());
        assertEquals(toDate, range.getToDate());
    }

    @Test
    void shouldTreatWeekendAsNonTradingDay() {
        assertFalse(WorkingDayDateRangeUtil.isTradingDay(LocalDate.of(2026, 7, 25)));
        assertTrue(WorkingDayDateRangeUtil.isTradingDay(LocalDate.of(2026, 7, 27)));
    }
}

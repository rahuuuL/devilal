package com.terminal_devilal.utils;

import java.time.DayOfWeek;
import java.time.LocalDate;

public final class WorkingDayDateRangeUtil {

    private WorkingDayDateRangeUtil() {
    }

    public static DateRange calculateDateRange(LocalDate toDate, int workingDays) {
        if (toDate == null) {
            throw new IllegalArgumentException("toDate must not be null");
        }
        if (workingDays <= 0) {
            throw new IllegalArgumentException("workingDays must be positive");
        }

        int weekendDays = (workingDays / 5) * 2;
        int calendarDays = workingDays + weekendDays;
        return new DateRange(toDate.minusDays(calendarDays), toDate);
    }

    public static LocalDate calculateFromDate(LocalDate toDate, int workingDays) {
        return calculateDateRange(toDate, workingDays).getFromDate();
    }

    public static boolean isTradingDay(LocalDate date) {
        if (date == null) {
            throw new IllegalArgumentException("date must not be null");
        }
        DayOfWeek dayOfWeek = date.getDayOfWeek();
        return dayOfWeek != DayOfWeek.SATURDAY && dayOfWeek != DayOfWeek.SUNDAY;
    }

    public static final class DateRange {
        private final LocalDate fromDate;
        private final LocalDate toDate;

        public DateRange(LocalDate fromDate, LocalDate toDate) {
            this.fromDate = fromDate;
            this.toDate = toDate;
        }

        public LocalDate getFromDate() {
            return fromDate;
        }

        public LocalDate getToDate() {
            return toDate;
        }
    }
}

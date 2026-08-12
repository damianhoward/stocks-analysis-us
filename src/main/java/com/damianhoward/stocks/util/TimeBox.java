package com.damianhoward.stocks.util;

import org.joda.time.DateTimeConstants;
import org.joda.time.LocalDate;

public class TimeBox {

    private static LocalDate localDate;

    public static LocalDate getPreviousDay() {
        return weekendAdjustment(TimeBox.getToday().minusDays(1));
    }

    public static LocalDate getPreviousFriday() {
        LocalDate today = TimeBox.getToday();
        if (today.getDayOfWeek() <= DateTimeConstants.FRIDAY) {
            today = today.minusWeeks(1).withDayOfWeek(DateTimeConstants.FRIDAY);
        }
        return today.withDayOfWeek(DateTimeConstants.FRIDAY);
    }

    public static LocalDate getPreviousQuarter() {
        LocalDate today = TimeBox.getToday();
        if (today.getMonthOfYear() < 4) {
            return weekendAdjustment(new LocalDate(today.minusYears(1).getYear(), 12, 31));
        } else {
            int quarterMonth = ((today.getMonthOfYear() - 1) / 3) * 3;
            return weekendAdjustment(new LocalDate(today.getYear(), quarterMonth, quarterMonth == 3 ? 31 : 30));
        }
    }

    public static LocalDate getPreviousYear() {
        LocalDate today = TimeBox.getToday();
        return weekendAdjustment(new LocalDate(today.minusYears(1).getYear(), 12, 31));
    }

    public static LocalDate getToday() {
        if (localDate == null) {
            return LocalDate.now();
        }
        return localDate;
    }

    private static LocalDate weekendAdjustment(LocalDate date) {
        if (date.getDayOfWeek() == 6) {
            date = date.minusDays(1);
        } else if (date.getDayOfWeek() == 7) {
            date = date.minusDays(2);
        }
        return date;
    }

    public static void setToday(LocalDate localDate) {
        TimeBox.localDate = localDate;
    }

}

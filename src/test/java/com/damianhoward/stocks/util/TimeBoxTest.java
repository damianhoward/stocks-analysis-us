package com.damianhoward.stocks.util;

import org.joda.time.LocalDate;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

public class TimeBoxTest {

    @Test
    public void testPreviousDayIfSaturday() {
        TimeBox.setToday(new LocalDate(2017, 5, 27));
        assertEquals(new LocalDate(2017, 5, 26), TimeBox.getPreviousDay());
    }

    @Test
    public void testPreviousDayIfSunday() {
        TimeBox.setToday(new LocalDate(2017, 5, 28));
        assertEquals(new LocalDate(2017, 5, 26), TimeBox.getPreviousDay());
    }

    @Test
    public void testPreviousDayIfMonday() {
        TimeBox.setToday(new LocalDate(2017, 5, 29));
        assertEquals(new LocalDate(2017, 5, 26), TimeBox.getPreviousDay());
    }

    @Test
    public void testPreviousFridayIfMonday() {
        TimeBox.setToday(new LocalDate(2017, 5, 29));
        assertEquals(new LocalDate(2017, 5, 26), TimeBox.getPreviousFriday());
    }

    @Test
    public void testPreviousFridayIfFriday() {
        TimeBox.setToday(new LocalDate(2017, 6, 2));
        assertEquals(new LocalDate(2017, 5, 26), TimeBox.getPreviousFriday());
    }

    @Test
    public void testPreviousFirstQuarter() {
        // quarter 1
        TimeBox.setToday(new LocalDate(2017, 2, 28));
        assertEquals(new LocalDate(2016, 12, 30), TimeBox.getPreviousQuarter());
    }

    @Test
    public void testPreviousSecondQuarter() {
        // quarter 2
        TimeBox.setToday(new LocalDate(2017, 4, 1));
        assertEquals(new LocalDate(2017, 3, 31), TimeBox.getPreviousQuarter());

        // quarter 2
        TimeBox.setToday(new LocalDate(2017, 6, 15));
        assertEquals(new LocalDate(2017, 3, 31), TimeBox.getPreviousQuarter());
    }

    @Test
    public void testPreviousThirdQuarter() {
        // quarter 3
        TimeBox.setToday(new LocalDate(2017, 7, 1));
        assertEquals(new LocalDate(2017, 6, 30), TimeBox.getPreviousQuarter());

        // quarter 3
        TimeBox.setToday(new LocalDate(2017, 9, 26));
        assertEquals(new LocalDate(2017, 6, 30), TimeBox.getPreviousQuarter());
    }

    @Test
    public void testPreviousFourthQuarter() {
        // quarter 4
        TimeBox.setToday(new LocalDate(2017, 10, 1));
        assertEquals(new LocalDate(2017, 9, 29), TimeBox.getPreviousQuarter());

        TimeBox.setToday(new LocalDate(2017, 12, 17));
        assertEquals(new LocalDate(2017, 9, 29), TimeBox.getPreviousQuarter());
    }

    @Test
    public void testPreviousYear() {
        TimeBox.setToday(new LocalDate(2017, 2, 28));
        assertEquals(new LocalDate(2016, 12, 30), TimeBox.getPreviousYear());
    }

    @Test
    public void getTodayReturnsNowWhenNotPinned() {
        TimeBox.setToday(null);
        assertNotNull(TimeBox.getToday());
    }

    @Test
    public void previousFridayWhenTodayIsWeekendStaysInSameWeek() {
        // Saturday: day-of-week (6) is past Friday, so no extra week is subtracted
        TimeBox.setToday(new LocalDate(2017, 5, 27));
        assertEquals(new LocalDate(2017, 5, 26), TimeBox.getPreviousFriday());
    }

}

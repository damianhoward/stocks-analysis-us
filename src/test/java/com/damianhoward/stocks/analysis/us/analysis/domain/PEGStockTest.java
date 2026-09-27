package com.damianhoward.stocks.analysis.us.analysis.domain;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class PEGStockTest {

    @Test
    void categorisedCarriesOnlyTheCategory() {
        PEGStock s = PEGStock.categorised("20 Reuters Lookup Invalid");

        assertEquals("20 Reuters Lookup Invalid", s.category());
        assertEquals(PegRatios.NONE, s.ratios());
    }

    @Test
    void roundingKeepsFourPlacesHalfUpAndLeavesAbsentRatiosAbsent() {
        PegRatios ratios = new PegRatios(
                new BigDecimal("1.23455"), new BigDecimal("1.23454"), null, BigDecimal.TEN, null, null);

        PegRatios rounded = ratios.rounded();

        assertEquals(new BigDecimal("1.2346"), rounded.thisYearEstimatePE());
        assertEquals(new BigDecimal("1.2345"), rounded.nextYearEstimatePE());
        assertNull(rounded.thisYearEPSGrowth());
        assertEquals(new BigDecimal("10.0000"), rounded.nextYearEPSGrowth());
    }
}

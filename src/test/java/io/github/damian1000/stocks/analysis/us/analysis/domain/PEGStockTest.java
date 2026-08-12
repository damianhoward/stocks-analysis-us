package io.github.damian1000.stocks.analysis.us.analysis.domain;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PEGStockTest {

    /** Only the two components compareTo reads; the rest are legitimately absent here. */
    private static PEGStock ranked(String category, double nextYearPEG) {
        return new PEGStock(null, null, null, null, null, null, BigDecimal.valueOf(nextYearPEG), category);
    }

    @Test
    void accessorsAndToStringRoundTrip() {
        PEGStock s = new PEGStock(
                "ZCS-1",
                null,
                null,
                null,
                null,
                BigDecimal.valueOf(1.1),
                BigDecimal.valueOf(0.9),
                "B");

        assertEquals("ZCS-1", s.zacksCode());
        assertEquals(BigDecimal.valueOf(1.1), s.thisYearPEG());
        assertEquals("B", s.category());
        assertTrue(s.toString().contains("ZCS-1"));
    }

    @Test
    void categorisedCarriesOnlyTheCategory() {
        PEGStock s = PEGStock.categorised("20 Reuters Lookup Invalid");

        assertEquals("20 Reuters Lookup Invalid", s.category());
        assertNull(s.zacksCode());
        assertNull(s.thisYearPEG());
        assertNull(s.nextYearPEG());
    }

    @Test
    void compareToOrdersByCategoryThenByNextYearPegAscending() {
        PEGStock a = ranked("A", 0.5);
        PEGStock aHigher = ranked("A", 1.5);
        PEGStock b = ranked("B", 0.1);

        List<PEGStock> list = new ArrayList<>(List.of(b, aHigher, a));
        Collections.sort(list);

        assertEquals(a, list.get(0)); // category A, lower PEG first
        assertEquals(aHigher, list.get(1));
        assertEquals(b, list.get(2));
    }
}

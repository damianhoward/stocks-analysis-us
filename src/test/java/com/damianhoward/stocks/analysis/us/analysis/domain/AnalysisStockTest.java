package com.damianhoward.stocks.analysis.us.analysis.domain;

import com.damianhoward.stocks.analysis.us.sectormapping.domain.ZacksSectorMapping;
import com.damianhoward.stocks.analysis.us.stocklookup.domain.StockLookup;
import com.damianhoward.stocks.analysis.us.zackscode.domain.ZacksCode;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static com.damianhoward.stocks.analysis.us.stocklookup.domain.QuoteBuilder.aQuote;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class AnalysisStockTest {

    private static final LocalDate DATE = LocalDate.of(2024, 1, 2);

    private static AnalysisStock ranked(String category, String nextYearPEG) {
        PegRatios ratios = new PegRatios(null, null, null, null, null, nextYearPEG == null ? null : new BigDecimal(nextYearPEG));
        return AnalysisStock.of(
                DATE, StockLookup.quoted(DATE, "ZC", aQuote().build()), null, null, new PEGStock(ratios, category));
    }

    @Test
    void assemblesTheRowFromTheLookupListingAndSector() {
        StockLookup lookup = StockLookup.quoted(DATE, "ZC", aQuote().company("Acme Corp").build());
        ZacksCode listing = ZacksCode.of(DATE, "Software", "ZC", "Acme Zacks Co");
        ZacksSectorMapping sector = ZacksSectorMapping.of(DATE, "Tech", "Apps", "Software");
        PEGStock peg = new PEGStock(
                new PegRatios(null, null, null, null, new BigDecimal("0.123456"), null), "00 Good");

        AnalysisStock row = AnalysisStock.of(DATE, lookup, listing, sector, peg);

        assertNull(row.id());
        assertEquals("ZC", row.zacksCode());
        assertEquals("Acme Zacks Co", row.zacksCompany());
        assertEquals("Tech", row.sectorGroup());
        assertEquals("Apps", row.mediumIndustryGroup());
        assertEquals("Software", row.industry());
        assertEquals("Acme Corp", row.quote().company());
        assertEquals(new BigDecimal("0.1235"), row.ratios().thisYearPEG());
        assertEquals("00 Good", row.category());
    }

    @Test
    void anUnmatchedStockKeepsItsQuoteAndLeavesClassificationEmpty() {
        StockLookup failed = StockLookup.failed(DATE, "ZC", "timed out");

        AnalysisStock row = AnalysisStock.of(DATE, failed, null, null, PEGStock.categorised("20 Reuters Lookup Invalid"));

        assertNull(row.zacksCompany());
        assertNull(row.sectorGroup());
        assertEquals("timed out", row.errorMessage());
    }

    @Test
    void ordersByCategoryThenByNextYearPegHighestFirstWithAbsentPegsLast() {
        AnalysisStock a = ranked("A", "1.0");
        AnalysisStock aHigh = ranked("A", "2.0");
        AnalysisStock aNone = ranked("A", null);
        AnalysisStock b = ranked("B", "0.5");

        List<AnalysisStock> list = new ArrayList<>(List.of(b, aNone, a, aHigh));
        Collections.sort(list);

        assertEquals(List.of(aHigh, a, aNone, b), list);
    }
}

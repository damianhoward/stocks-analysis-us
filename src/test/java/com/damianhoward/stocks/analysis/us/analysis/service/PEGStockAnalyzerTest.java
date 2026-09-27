package com.damianhoward.stocks.analysis.us.analysis.service;

import com.damianhoward.stocks.analysis.us.analysis.domain.PEGStock;
import com.damianhoward.stocks.analysis.us.analysis.domain.PegRatios;
import com.damianhoward.stocks.analysis.us.stocklookup.domain.Quote;
import com.damianhoward.stocks.analysis.us.stocklookup.domain.StockLookup;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static com.damianhoward.stocks.analysis.us.stocklookup.domain.QuoteBuilder.aQuote;
import static com.damianhoward.stocks.analysis.us.stocklookup.domain.QuoteBuilder.withEarnings;
import static org.junit.jupiter.api.Assertions.assertEquals;

class PEGStockAnalyzerTest {

    private static final LocalDate DATE = LocalDate.of(2024, 1, 2);

    private final PEGStockAnalyzer stockAnalyzer = new PEGStockAnalyzer();

    private PEGStock analyse(Quote quote) {
        return stockAnalyzer.analyzeStocks(StockLookup.quoted(DATE, "ZC", quote));
    }

    @Test
    void falling_then_recovering_earnings() {
        PEGStock pegStock = analyse(withEarnings("3.869", "0.545", "0.4684", "0.4936").build());
        PegRatios ratios = pegStock.ratios();

        assertEquals(8.26, ratios.thisYearEstimatePE().doubleValue(), 0.01); // price / thisYearEPS
        assertEquals(7.84, ratios.nextYearEstimatePE().doubleValue(), 0.01); // price / nextYearEPS
        assertEquals(-14.06, ratios.thisYearEPSGrowth().doubleValue(), 0.01); // (this - last) / last
        assertEquals(5.38, ratios.nextYearEPSGrowth().doubleValue(), 0.01); // (next - this) / this
        assertEquals(-0.59, ratios.thisYearPEG().doubleValue(), 0.01); // thisYearPE / thisYearEPSGrowth
        assertEquals(1.46, ratios.nextYearPEG().doubleValue(), 0.01); // nextYearPE / nextYearEPSGrowth
        assertEquals("00 Good", pegStock.category());
    }

    @Test
    void strongly_growing_earnings() {
        PEGStock pegStock = analyse(withEarnings("35.89", "0.545", "1.59", "1.84").build());
        PegRatios ratios = pegStock.ratios();

        assertEquals(22.57, ratios.thisYearEstimatePE().doubleValue(), 0.01);
        assertEquals(19.51, ratios.nextYearEstimatePE().doubleValue(), 0.01);
        assertEquals(191.74, ratios.thisYearEPSGrowth().doubleValue(), 0.01);
        assertEquals(15.72, ratios.nextYearEPSGrowth().doubleValue(), 0.01);
        assertEquals(0.12, ratios.thisYearPEG().doubleValue(), 0.01);
        assertEquals(1.24, ratios.nextYearPEG().doubleValue(), 0.01);
        assertEquals("00 Good", pegStock.category());
    }

    @Test
    void a_quote_without_earnings_short_circuits_to_category_20() {
        PEGStock peg = analyse(aQuote().build());

        assertEquals("20 Reuters Lookup Invalid", peg.category());
        assertEquals(PegRatios.NONE, peg.ratios());
    }

    @Test
    void zero_earnings_leave_both_pegs_undefined() {
        // A zero EPS makes every division undefined, which is missing data rather than a bad score.
        PEGStock peg = analyse(withEarnings("10", "0", "0", "0").build());

        assertEquals("10 Missing Stats", peg.category());
    }
}

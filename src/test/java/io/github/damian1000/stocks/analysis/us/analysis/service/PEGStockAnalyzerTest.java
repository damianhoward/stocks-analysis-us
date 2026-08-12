package io.github.damian1000.stocks.analysis.us.analysis.service;

import io.github.damian1000.stocks.analysis.us.stocklookup.domain.StockLookup;
import io.github.damian1000.stocks.analysis.us.analysis.domain.PEGStock;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class PEGStockAnalyzerTest {

    private PEGStockAnalyzer stockAnalyzer = new PEGStockAnalyzer();

    @Test
    public void testChris3LGRP() {
        BigDecimal marketCap = BigDecimal.valueOf(3.70971);
        String yearEnding = "3";
        BigDecimal beta = BigDecimal.valueOf(1.2);
        BigDecimal price = BigDecimal.valueOf(3.869);
        BigDecimal lastYearEPS = BigDecimal.valueOf(0.545);
        BigDecimal thisYearEstimateEPS = BigDecimal.valueOf(0.4684);
        BigDecimal nextYearEstimateEPS = BigDecimal.valueOf(0.4936);
        String earningAboveEstimates = "0/0";

        StockLookup stock = StockLookup.builder()
                .marketCap(marketCap)
                .yearEnding(yearEnding)
                .beta(beta)
                .price(price)
                .lastYearEPS(lastYearEPS)
                .thisYearEstimateEPS(thisYearEstimateEPS)
                .nextYearEstimateEPS(nextYearEstimateEPS)
                .earningAboveEstimates(earningAboveEstimates)
                .build();

        PEGStock pegStock = stockAnalyzer.analyzeStocks(stock);

        assertEquals(8.26, pegStock.thisYearEstimatePE().doubleValue(), 0.01); //  price / thisYearEPS
        assertEquals(7.84, pegStock.nextYearEstimatePE().doubleValue(), 0.01); //  price / nextYearEPS

        assertEquals(-14.06, pegStock.thisYearEPSGrowth().doubleValue(), 0.01); // (thisYearEPS - lastYearEPS) / thisYearEPS
        assertEquals(5.38, pegStock.nextYearEPSGrowth().doubleValue(), 0.01); // (thisYearEPS - thisYearEPS) / thisYearEPS

        assertEquals(-0.59, pegStock.thisYearPEG().doubleValue(), 0.01); // thisYearPE / thisYearEPSGrowth
        assertEquals(1.46, pegStock.nextYearPEG().doubleValue(), 0.01); // nextYearEPS / nextYearEPSGrowth

        assertEquals("00 Good", pegStock.category());
    }

    @Test
    public void invalidLookupShortCircuitsToCategory20() {
        StockLookup invalid = StockLookup.builder().build(); // no price/EPS
        PEGStock peg = stockAnalyzer.analyzeStocks(invalid);
        assertEquals("20 Reuters Lookup Invalid", peg.category());
    }

    @Test
    public void missingStatsCategoryWhenBothPegNull() {
        // Zero EPS values cause divide() to return null -> both PEGs null -> "10 Missing Stats"
        StockLookup stock = StockLookup.builder()
                .marketCap(BigDecimal.valueOf(1))
                .yearEnding("12")
                .beta(BigDecimal.ONE)
                .price(BigDecimal.valueOf(10))
                .lastYearEPS(BigDecimal.ZERO)
                .thisYearEstimateEPS(BigDecimal.ZERO)
                .nextYearEstimateEPS(BigDecimal.ZERO)
                .earningAboveEstimates("0/0")
                .build();
        PEGStock peg = stockAnalyzer.analyzeStocks(stock);
        assertEquals("10 Missing Stats", peg.category());
    }

    @Test
    public void testChrisACCOR() {
        BigDecimal marketCap = BigDecimal.valueOf(8.13523);
        String yearEnding = "12";
        BigDecimal beta = BigDecimal.valueOf(1.3);
        BigDecimal price = BigDecimal.valueOf(35.89);
        BigDecimal lastYearEPS = BigDecimal.valueOf(0.545);
        BigDecimal thisYearEstimateEPS = BigDecimal.valueOf(1.59);
        BigDecimal nextYearEstimateEPS = BigDecimal.valueOf(1.84);
        String earningAboveEstimates = "0/0";

        StockLookup stock = StockLookup.builder()
                .marketCap(marketCap)
                .yearEnding(yearEnding)
                .beta(beta)
                .price(price)
                .lastYearEPS(lastYearEPS)
                .thisYearEstimateEPS(thisYearEstimateEPS)
                .nextYearEstimateEPS(nextYearEstimateEPS)
                .earningAboveEstimates(earningAboveEstimates)
                .build();

        PEGStock pegStock = stockAnalyzer.analyzeStocks(stock);

        assertEquals(22.57, pegStock.thisYearEstimatePE().doubleValue(), 0.01);
        assertEquals(19.51, pegStock.nextYearEstimatePE().doubleValue(), 0.01);

        assertEquals(191.74, pegStock.thisYearEPSGrowth().doubleValue(), 0.01);
        assertEquals(15.72, pegStock.nextYearEPSGrowth().doubleValue(), 0.01);

        assertEquals(0.12, pegStock.thisYearPEG().doubleValue(), 0.01);
        assertEquals(1.24, pegStock.nextYearPEG().doubleValue(), 0.01);

        assertEquals("00 Good", pegStock.category());
    }

}

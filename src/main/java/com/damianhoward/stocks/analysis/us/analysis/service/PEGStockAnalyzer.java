package com.damianhoward.stocks.analysis.us.analysis.service;

import com.damianhoward.stocks.analysis.us.stocklookup.domain.StockLookup;
import com.damianhoward.stocks.analysis.us.analysis.domain.PEGStock;
import com.damianhoward.stocks.util.Decimals;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

import static com.damianhoward.stocks.util.Decimals.diffAsPercentage;
import static com.damianhoward.stocks.util.Decimals.divide;

@Component
public class PEGStockAnalyzer {

    public PEGStock analyzeStocks(StockLookup stockLookup) {
        if (!stockLookup.isValid()) {
            return PEGStock.categorised("20 Reuters Lookup Invalid");
        }

        BigDecimal price = stockLookup.getPrice();

        BigDecimal lastYearEPS = stockLookup.getLastYearEPS();
        BigDecimal thisYearEstimateEPS = stockLookup.getThisYearEstimateEPS();
        BigDecimal nextYearEstimateEPS = stockLookup.getNextYearEstimateEPS();

        BigDecimal thisYearEstimatePE = Decimals.divide(price, thisYearEstimateEPS);
        BigDecimal nextYearEstimatePE = Decimals.divide(price, nextYearEstimateEPS);

        BigDecimal thisYearEPSGrowth = diffAsPercentage(lastYearEPS, thisYearEstimateEPS);
        BigDecimal nextYearEPSGrowth = diffAsPercentage(thisYearEstimateEPS, nextYearEstimateEPS);

        BigDecimal thisYearPEG = divide(thisYearEstimatePE, thisYearEPSGrowth);
        BigDecimal nextYearPEG = divide(nextYearEstimatePE, nextYearEPSGrowth);

        // Neither PEG computable means the inputs were missing, not that the stock scored badly.
        String category = thisYearPEG == null && nextYearPEG == null ? "10 Missing Stats" : "00 Good";

        return new PEGStock(
                stockLookup.getZacksCode(),
                thisYearEstimatePE,
                nextYearEstimatePE,
                thisYearEPSGrowth,
                nextYearEPSGrowth,
                thisYearPEG,
                nextYearPEG,
                category);
    }

}

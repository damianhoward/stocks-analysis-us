package com.damianhoward.stocks.analysis.us.analysis.service;

import com.damianhoward.stocks.analysis.us.analysis.domain.PEGStock;
import com.damianhoward.stocks.analysis.us.analysis.domain.PegRatios;
import com.damianhoward.stocks.analysis.us.stocklookup.domain.Quote;
import com.damianhoward.stocks.analysis.us.stocklookup.domain.StockLookup;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

import static com.damianhoward.stocks.util.Decimals.diffAsPercentage;
import static com.damianhoward.stocks.util.Decimals.divide;

@Component
public class PEGStockAnalyzer {

    public PEGStock analyzeStocks(StockLookup stockLookup) {
        Quote quote = stockLookup.quote();
        if (!quote.hasEarnings()) {
            return PEGStock.categorised("20 Reuters Lookup Invalid");
        }

        BigDecimal thisYearEstimatePE = divide(quote.price(), quote.thisYearEstimateEPS());
        BigDecimal nextYearEstimatePE = divide(quote.price(), quote.nextYearEstimateEPS());

        BigDecimal thisYearEPSGrowth = diffAsPercentage(quote.lastYearEPS(), quote.thisYearEstimateEPS());
        BigDecimal nextYearEPSGrowth = diffAsPercentage(quote.thisYearEstimateEPS(), quote.nextYearEstimateEPS());

        BigDecimal thisYearPEG = divide(thisYearEstimatePE, thisYearEPSGrowth);
        BigDecimal nextYearPEG = divide(nextYearEstimatePE, nextYearEPSGrowth);

        // Neither PEG computable means the inputs were missing, not that the stock scored badly.
        String category = thisYearPEG == null && nextYearPEG == null ? "10 Missing Stats" : "00 Good";

        return new PEGStock(
                new PegRatios(
                        thisYearEstimatePE,
                        nextYearEstimatePE,
                        thisYearEPSGrowth,
                        nextYearEPSGrowth,
                        thisYearPEG,
                        nextYearPEG),
                category);
    }

}

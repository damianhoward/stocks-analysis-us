package com.damianhoward.stocks.analysis.us.stocklookup.domain;

import java.math.BigDecimal;

/** Builds a {@link Quote} for a test, naming only the fields the test is about. */
public final class QuoteBuilder {

    private String company;
    private String currency;
    private BigDecimal marketCap;
    private String yearEnding;
    private BigDecimal beta;
    private BigDecimal price;
    private BigDecimal targetPrice;
    private BigDecimal lastYearEPS;
    private BigDecimal lastYearPE;
    private BigDecimal thisYearEstimateEPS;
    private BigDecimal nextYearEstimateEPS;
    private String earningAboveEstimates;
    private BigDecimal recommendationRating;

    private QuoteBuilder() {}

    public static QuoteBuilder aQuote() {
        return new QuoteBuilder();
    }

    /** A quote carrying the price and the three EPS figures the PEG ratios are computed from. */
    public static QuoteBuilder withEarnings(String price, String lastYearEPS, String thisYearEPS, String nextYearEPS) {
        return aQuote()
                .price(new BigDecimal(price))
                .lastYearEPS(new BigDecimal(lastYearEPS))
                .thisYearEstimateEPS(new BigDecimal(thisYearEPS))
                .nextYearEstimateEPS(new BigDecimal(nextYearEPS));
    }

    public QuoteBuilder company(String value) {
        company = value;
        return this;
    }

    public QuoteBuilder currency(String value) {
        currency = value;
        return this;
    }

    public QuoteBuilder marketCap(BigDecimal value) {
        marketCap = value;
        return this;
    }

    public QuoteBuilder yearEnding(String value) {
        yearEnding = value;
        return this;
    }

    public QuoteBuilder beta(BigDecimal value) {
        beta = value;
        return this;
    }

    public QuoteBuilder price(BigDecimal value) {
        price = value;
        return this;
    }

    public QuoteBuilder targetPrice(BigDecimal value) {
        targetPrice = value;
        return this;
    }

    public QuoteBuilder lastYearEPS(BigDecimal value) {
        lastYearEPS = value;
        return this;
    }

    public QuoteBuilder lastYearPE(BigDecimal value) {
        lastYearPE = value;
        return this;
    }

    public QuoteBuilder thisYearEstimateEPS(BigDecimal value) {
        thisYearEstimateEPS = value;
        return this;
    }

    public QuoteBuilder nextYearEstimateEPS(BigDecimal value) {
        nextYearEstimateEPS = value;
        return this;
    }

    public QuoteBuilder earningAboveEstimates(String value) {
        earningAboveEstimates = value;
        return this;
    }

    public QuoteBuilder recommendationRating(BigDecimal value) {
        recommendationRating = value;
        return this;
    }

    public Quote build() {
        return new Quote(
                company,
                currency,
                marketCap,
                yearEnding,
                beta,
                price,
                targetPrice,
                lastYearEPS,
                lastYearPE,
                thisYearEstimateEPS,
                nextYearEstimateEPS,
                earningAboveEstimates,
                recommendationRating);
    }
}

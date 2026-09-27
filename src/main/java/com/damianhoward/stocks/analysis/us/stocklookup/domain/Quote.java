package com.damianhoward.stocks.analysis.us.stocklookup.domain;

import org.springframework.data.relational.core.mapping.Column;

import java.math.BigDecimal;

/**
 * What the quote source reports about one stock: its name, its market data and the earnings
 * estimates the analysis ratios are built from. Every field may be absent, because the source
 * omits what it does not have.
 *
 * <p>Embedded in both {@link StockLookup} and the analysis row, whose tables carry the same
 * columns under the same names.
 */
public record Quote(
        @Column("company") String company,
        @Column("currency") String currency,
        @Column("market_cap") BigDecimal marketCap,
        @Column("year_ending") String yearEnding,
        @Column("beta") BigDecimal beta,
        @Column("price") BigDecimal price,
        @Column("target_price") BigDecimal targetPrice,
        @Column("last_year_eps") BigDecimal lastYearEPS,
        @Column("last_year_pe") BigDecimal lastYearPE,
        @Column("this_year_estimate_eps") BigDecimal thisYearEstimateEPS,
        @Column("next_year_estimate_eps") BigDecimal nextYearEstimateEPS,
        @Column("earning_above_estimates") String earningAboveEstimates,
        @Column("recommendation_rating") BigDecimal recommendationRating) {

    /** A lookup that failed reports nothing. */
    public static final Quote NONE =
            new Quote(null, null, null, null, null, null, null, null, null, null, null, null, null);

    /** Whether the price and the three earnings figures the ratios divide by are all present. */
    public boolean hasEarnings() {
        return price != null && lastYearEPS != null && thisYearEstimateEPS != null && nextYearEstimateEPS != null;
    }

    /**
     * The same quote with every money amount multiplied by {@code rate}. Ratios — P/E, beta and
     * the recommendation — have no currency and are unchanged.
     */
    public Quote convertedTo(String targetCurrency, double rate) {
        BigDecimal factor = BigDecimal.valueOf(rate);
        return new Quote(
                company,
                targetCurrency,
                scale(marketCap, factor),
                yearEnding,
                beta,
                scale(price, factor),
                scale(targetPrice, factor),
                scale(lastYearEPS, factor),
                lastYearPE,
                scale(thisYearEstimateEPS, factor),
                scale(nextYearEstimateEPS, factor),
                earningAboveEstimates,
                recommendationRating);
    }

    private static BigDecimal scale(BigDecimal value, BigDecimal factor) {
        return value == null ? null : value.multiply(factor);
    }
}

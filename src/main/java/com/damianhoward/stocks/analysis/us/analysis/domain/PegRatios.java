package com.damianhoward.stocks.analysis.us.analysis.domain;

import org.springframework.data.relational.core.mapping.Column;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * The ratios a PEG analysis derives from a quote. Every one is nullable by design: a missing EPS
 * estimate makes a PE, a growth rate and a PEG undefined rather than zero.
 */
public record PegRatios(
        @Column("this_year_estimate_pe") BigDecimal thisYearEstimatePE,
        @Column("next_year_estimate_pe") BigDecimal nextYearEstimatePE,
        @Column("this_year_eps_growth") BigDecimal thisYearEPSGrowth,
        @Column("next_year_eps_growth") BigDecimal nextYearEPSGrowth,
        @Column("this_year_peg") BigDecimal thisYearPEG,
        @Column("next_year_peg") BigDecimal nextYearPEG) {

    public static final PegRatios NONE = new PegRatios(null, null, null, null, null, null);

    private static final int STORED_SCALE = 4;

    /** The ratios at the four decimal places the report stores and shows. */
    public PegRatios rounded() {
        return new PegRatios(
                round(thisYearEstimatePE),
                round(nextYearEstimatePE),
                round(thisYearEPSGrowth),
                round(nextYearEPSGrowth),
                round(thisYearPEG),
                round(nextYearPEG));
    }

    private static BigDecimal round(BigDecimal value) {
        return value == null ? null : value.setScale(STORED_SCALE, RoundingMode.HALF_UP);
    }
}

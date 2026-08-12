package com.damianhoward.stocks.analysis.us.analysis.domain;

import java.math.BigDecimal;

/**
 * A stock's PEG analysis: the derived ratios and the category the run sorts and reports by.
 *
 * <p>Every ratio is nullable by design. A missing EPS estimate makes a PE, a growth rate and a PEG
 * undefined rather than zero, and the categories exist to say which of those happened — so the
 * components stay boxed and the analyzer decides the category from what it could actually compute.
 */
public record PEGStock(
        String zacksCode,
        BigDecimal thisYearEstimatePE,
        BigDecimal nextYearEstimatePE,
        BigDecimal thisYearEPSGrowth,
        BigDecimal nextYearEPSGrowth,
        BigDecimal thisYearPEG,
        BigDecimal nextYearPEG,
        String category)
        implements Comparable<PEGStock> {

    /**
     * A result that carries only a category, for the cases where no ratio could be computed at
     * all — an invalid lookup has nothing to divide.
     */
    public static PEGStock categorised(String category) {
        return new PEGStock(null, null, null, null, null, null, null, category);
    }

    /** Orders by category, then by next year's PEG within it — the report's reading order. */
    @Override
    public int compareTo(PEGStock o) {
        int result = category.compareTo(o.category);
        if (result == 0) {
            result = nextYearPEG.compareTo(o.nextYearPEG);
        }
        return result;
    }
}

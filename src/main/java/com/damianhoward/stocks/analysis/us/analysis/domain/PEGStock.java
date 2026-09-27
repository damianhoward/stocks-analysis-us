package com.damianhoward.stocks.analysis.us.analysis.domain;

/**
 * A stock's PEG analysis: the ratios that could be computed and the category the run sorts and
 * reports by. The category says which ratios were missing and why, so the analyzer decides it from
 * what it could actually compute.
 */
public record PEGStock(PegRatios ratios, String category) {

    /** A result that carries only a category, for a lookup with nothing to divide. */
    public static PEGStock categorised(String category) {
        return new PEGStock(PegRatios.NONE, category);
    }
}

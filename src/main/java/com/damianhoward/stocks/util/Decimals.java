package com.damianhoward.stocks.util;

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;

/**
 * Null-tolerant {@link BigDecimal} arithmetic, which is the concept these share rather than
 * "number utilities". Every value in this pipeline arrives from a scraped page or a third-party
 * feed and may legitimately be absent, so each operation answers null for an input it cannot
 * compute rather than throwing — an absent ratio is data, not a defect.
 *
 * <p>Division guards against a zero denominator for the same reason: a stock with zero earnings
 * has no P/E, and that is a fact to record rather than an arithmetic error to propagate.
 */
public class Decimals {

    public static int compare(BigDecimal one, BigDecimal two) {
        if (one == null && two == null) {
            return 0;
        }
        if (one == null) {
            return -1;
        }
        if (two == null) {
            return 1;
        }
        return one.compareTo(two);
    }

    public static BigDecimal divide(BigDecimal one, BigDecimal two) {
        if (one != null && two != null) {
            if (two.compareTo(BigDecimal.ZERO) != 0) {
                return one.divide(two, MathContext.DECIMAL128);
            }
        }
        return null;
    }

    public static BigDecimal diffAsPercentage(BigDecimal one, BigDecimal two) {
        if (one != null && two != null && one.compareTo(BigDecimal.ZERO) != 0) {
            BigDecimal diff = two.subtract(one);
            return diff.divide(one, MathContext.DECIMAL128).multiply(BigDecimal.valueOf(100));
        }
        return null;
    }

    public static BigDecimal format(BigDecimal bd) {
        if (bd != null) {
            return bd.setScale(4, RoundingMode.HALF_UP);
        }
        return null;
    }
}

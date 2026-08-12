package com.damianhoward.stocks.util;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class DecimalsTest {

    @Test
    void compareHandlesNullsConsistently() {
        assertEquals(0, Decimals.compare(null, null));
        assertEquals(-1, Decimals.compare(null, BigDecimal.ONE));
        assertEquals(1, Decimals.compare(BigDecimal.ONE, null));
    }

    @Test
    void compareDelegatesToBigDecimal() {
        assertEquals(-1, Decimals.compare(BigDecimal.ONE, BigDecimal.TEN));
        assertEquals(0, Decimals.compare(BigDecimal.ONE, BigDecimal.ONE));
        assertEquals(1, Decimals.compare(BigDecimal.TEN, BigDecimal.ONE));
    }

    @Test
    void divideReturnsNullWhenEitherOperandIsNullOrZero() {
        assertNull(Decimals.divide(null, BigDecimal.ONE));
        assertNull(Decimals.divide(BigDecimal.ONE, null));
        assertNull(Decimals.divide(BigDecimal.ONE, BigDecimal.ZERO));
    }

    @Test
    void divideUsesDecimal128Precision() {
        BigDecimal result = Decimals.divide(BigDecimal.ONE, new BigDecimal("3"));
        // 1/3 to MathContext.DECIMAL128 has 34 significant digits.
        org.junit.jupiter.api.Assertions.assertTrue(result.toPlainString().startsWith("0.33333"));
    }

    @Test
    void diffAsPercentageGivesPercentChangeFromOneToTwo() {
        // From 100 to 110 = +10%.
        BigDecimal pct = Decimals.diffAsPercentage(new BigDecimal("100"), new BigDecimal("110"));
        assertEquals(0, pct.compareTo(new BigDecimal("10")));
    }

    @Test
    void diffAsPercentageReturnsNullOnNullInputsOrZeroBase() {
        assertNull(Decimals.diffAsPercentage(null, BigDecimal.ONE));
        assertNull(Decimals.diffAsPercentage(BigDecimal.ONE, null));
        assertNull(Decimals.diffAsPercentage(BigDecimal.ZERO, BigDecimal.ONE));
    }

    @Test
    void formatRoundsTo4DecimalPlacesHalfUp() {
        assertEquals(new BigDecimal("1.2346"), Decimals.format(new BigDecimal("1.23456789")));
        assertNull(Decimals.format(null));
    }
}

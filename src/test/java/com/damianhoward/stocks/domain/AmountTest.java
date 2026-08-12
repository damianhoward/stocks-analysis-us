package com.damianhoward.stocks.domain;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AmountTest {

    @Test
    void symbolicCurrencyMapsToIsoCode() {
        assertEquals("EUR", new Amount("€", BigDecimal.TEN).getCurrency());
        assertEquals("GBP", new Amount("£", BigDecimal.TEN).getCurrency());
        assertEquals("USD", new Amount("$", BigDecimal.TEN).getCurrency());
    }

    @Test
    void descriptiveNameMapsToIsoCode() {
        assertEquals("USD", new Amount("U.S. Dollars", BigDecimal.ONE).getCurrency());
        assertEquals("JPY", new Amount("Yen", BigDecimal.ONE).getCurrency());
        assertEquals("CNY", new Amount("Renminbi", BigDecimal.ONE).getCurrency());
    }

    @Test
    void unknownCurrencyPassesThrough() {
        assertEquals("XYZ", new Amount("XYZ", BigDecimal.ONE).getCurrency());
    }

    @Test
    void krDotIsNormalizedToKr() {
        // "kr." -> "kr" before lookup; "kr" isn't in the table so it passes through.
        assertEquals("kr", new Amount("kr.", BigDecimal.ONE).getCurrency());
    }

    @Test
    void britishPenceConvertsToGbpDividedBy100() {
        Amount p = new Amount("p", new BigDecimal("250"));
        assertEquals("GBP", p.getCurrency());
        assertEquals(0, p.getPrice().compareTo(new BigDecimal("2.50")));
    }

    @Test
    void britishPenceCanonicalGBpAlsoConverts() {
        Amount p = new Amount("GBp", new BigDecimal("1500"));
        assertEquals("GBP", p.getCurrency());
        assertEquals(0, p.getPrice().compareTo(new BigDecimal("15.00")));
    }

    @Test
    void isPriceZeroDistinguishesZeroFromOther() {
        assertTrue(new Amount("USD", BigDecimal.ZERO).isPriceZero());
        assertFalse(new Amount("USD", BigDecimal.ONE).isPriceZero());
    }

    @Test
    void isValidRequiresThreeLetterUppercaseCurrency() {
        assertTrue(new Amount("USD", BigDecimal.ONE).isValid());
        assertFalse(new Amount("Usd", BigDecimal.ONE).isValid(), "not uppercase");
        assertFalse(new Amount("USDD", BigDecimal.ONE).isValid(), "not 3 letters");
        assertFalse(new Amount("US", BigDecimal.ONE).isValid(), "too short");
    }

    @Test
    void divideReturnsRatioWhenBothPricesPresent() {
        Amount a = new Amount("USD", new BigDecimal("100"));
        Amount b = new Amount("USD", new BigDecimal("4"));
        assertEquals(0, a.divide(b).compareTo(new BigDecimal("25")));
    }

    @Test
    void divideByZeroReturnsNullToAvoidThrow() {
        Amount a = new Amount("USD", BigDecimal.ONE);
        Amount zero = new Amount("USD", BigDecimal.ZERO);
        assertNull(a.divide(zero));
    }

    @Test
    void divideHandlesNullPrices() {
        Amount a = new Amount();
        Amount b = new Amount();
        assertNull(a.divide(b));
    }

    @Test
    void createAmountFactoriesProduceEquivalentValuesTest() {
        Amount fromDouble = Amount.createAmount("USD", 12.5);
        Amount fromBigDecimal = Amount.createAmount("USD", new BigDecimal("12.5"));
        assertEquals("USD", fromDouble.getCurrency());
        assertEquals(0, fromDouble.getPrice().compareTo(fromBigDecimal.getPrice()));
    }
}

package com.damianhoward.stocks.util;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class FormatterTest {

    @Test
    void isInstantiable() {
        assertNotNull(new Formatter());
    }

    @Test
    void roundsToTwoDecimalPlaces() {
        assertEquals(1.23, Formatter.format(1.234));
        assertEquals(1.24, Formatter.format(1.235));
        assertEquals(0.0, Formatter.format(0.001));
        assertEquals(99.99, Formatter.format(99.994));
    }

    @Test
    void handlesNegativeAndLargeValues() {
        assertEquals(-1.23, Formatter.format(-1.234));
        assertEquals(123456.78, Formatter.format(123456.781));
    }
}

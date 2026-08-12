package com.damianhoward.stocks.util;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class DelimiterTest {

    @Test
    void constantsAreSet() {
        assertEquals("\t", Delimiter.TAB);
        assertEquals("\n", Delimiter.LINE_BREAK);
        // Touch the default constructor so the synthesized init shows as covered.
        assertNotNull(new Delimiter());
    }
}

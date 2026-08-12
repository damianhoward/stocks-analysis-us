package com.damianhoward.stocks.exception;

import org.junit.jupiter.api.Test;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.assertSame;

class DataRetrievalErrorTest {

    @Test
    void wrapsTheCause() {
        IOException cause = new IOException("boom");
        DataRetrievalError ex = new DataRetrievalError(cause);
        assertSame(cause, ex.getCause());
    }
}

package com.damianhoward.stocks.analysis.us.stocklookup.service.yahoo;

import static org.junit.jupiter.api.Assertions.assertTrue;

import com.damianhoward.stocks.exception.DataRetrievalError;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.DisabledIfEnvironmentVariable;

/**
 * Smoke test against the <em>real</em> Yahoo Finance quoteSummary API, exercising the live
 * cookie → crumb → quoteSummary gate end to end. This is the test that would have caught the
 * cookie-spec regression: the loopback fakes in {@link YahooFinanceClientTest} can't reproduce
 * Yahoo rejecting a cookie with an RFC 1123 {@code Expires}, so only a real call surfaces it.
 *
 * <p>It runs by default — Yahoo is effectively always up, and an always-on external check is what
 * actually guards this integration. Disable it (e.g. for offline work or a flaky network) by
 * setting the environment variable {@code YAHOO_LIVE_SKIP=true}; env vars are inherited by the
 * forked test JVM, whereas {@code -D} system properties are not.
 */
@Tag("live")
@DisabledIfEnvironmentVariable(named = "YAHOO_LIVE_SKIP", matches = "true")
class YahooFinanceClientLiveTest {

    @Test
    void fetchesRealQuoteSummaryForAapl() throws DataRetrievalError {
        String json = new YahooFinanceClient().fetchQuoteSummary("AAPL");

        assertTrue(json.contains("quoteSummary"), "expected a quoteSummary envelope, got: " + json);
        assertTrue(json.contains("longName"), "expected the price module's longName, got: " + json);
    }
}

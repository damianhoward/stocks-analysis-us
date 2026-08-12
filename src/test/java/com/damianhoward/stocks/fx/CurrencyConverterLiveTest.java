package com.damianhoward.stocks.fx;

import static org.junit.jupiter.api.Assertions.assertTrue;

import com.damianhoward.stocks.exception.DataRetrievalError;
import com.damianhoward.stocks.html.HtmlParser;
import com.damianhoward.stocks.html.HtmlRetriever;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.DisabledIfEnvironmentVariable;

/**
 * Smoke test against the <em>real</em> Frankfurter API. The mocked tests in
 * {@link CurrencyConverterTest} prove the EUR-leg division and parsing, but only a live call proves
 * the request shape and JSON layout still match what Frankfurter actually serves.
 *
 * <p>It runs by default — Frankfurter is keyless and effectively always up, and an always-on
 * external check is what actually guards this integration. Disable it (e.g. for offline work or a
 * flaky network) by setting the environment variable {@code FX_LIVE_SKIP=true}; env vars are
 * inherited by the forked test JVM, whereas {@code -D} system properties are not.
 */
@Tag("live")
@DisabledIfEnvironmentVariable(named = "FX_LIVE_SKIP", matches = "true")
class CurrencyConverterLiveTest {

    @Test
    void fetchesRealUsdToNzdRate() throws DataRetrievalError {
        CurrencyConverter converter =
                new CurrencyConverter(new HtmlRetriever(), new HtmlParser(), "https://api.frankfurter.dev/v2/rates");

        // USD/NZD is used by no mocked test, so the process-wide cache can't pre-seed it — this is a real fetch.
        double rate = converter.convert("USD", "NZD");

        // A broad band: just enough to catch a parse failure (0.0) or garbage, never tight enough to flake.
        assertTrue(rate > 0.5 && rate < 5.0, "expected a plausible USD->NZD rate, got: " + rate);
    }
}

package com.damianhoward.stocks.analysis.us.stocklookup.service;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

import java.util.Objects;

/**
 * How hard the lookup stage leans on Yahoo. {@code concurrency} is the size of the lookup pool,
 * which makes it the number of requests in flight at once; the sleep window is a per-lookup
 * courtesy delay in seconds on top of it.
 */
@ConfigurationProperties("stocks.analysis.us.stocklookup")
public record StockLookupSettings(@DefaultValue("8") int concurrency, SleepTime sleeptime) {

    /** The window has no default, so an application missing it fails at startup, not mid-run. */
    public StockLookupSettings {
        Objects.requireNonNull(sleeptime, "stocks.analysis.us.stocklookup.sleeptime must be set");
    }

    public record SleepTime(int min, int max) {
    }
}

package com.damianhoward.stocks.analysis.us.stocklookup.service.yahoo;

import com.damianhoward.stocks.exception.DataRetrievalError;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.CookieManager;
import java.net.CookiePolicy;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;

/**
 * Talks to Yahoo Finance's {@code quoteSummary} JSON API. Yahoo gates the API
 * behind a cookie + "crumb" pair: you fetch a cookie, exchange it for a crumb,
 * then pass the crumb on each request. Crumbs expire, so a 401 triggers a single
 * refresh-and-retry. This replaces scraping the (now removed) HTML analysis page.
 */
@Component
public class YahooFinanceClient {

    private static final Logger log = LoggerFactory.getLogger(YahooFinanceClient.class);

    private static final String MODULES = "price,summaryDetail,financialData,earningsTrend,earningsHistory";
    private static final String USER_AGENT =
            "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0 Safari/537.36";
    // Bounded, because one hung Yahoo socket would otherwise stall the whole lookup stage instead
    // of failing the symbol and moving on.
    private static final Duration CONNECT_TIMEOUT = Duration.ofSeconds(10);
    private static final Duration REQUEST_TIMEOUT = Duration.ofSeconds(30);

    private final String apiBaseUrl;
    private final String cookieSeedUrl;
    private final HttpClient httpClient;
    private volatile String crumb;

    public YahooFinanceClient() {
        this("https://query2.finance.yahoo.com", "https://fc.yahoo.com/");
    }

    // Visible for testing: lets a test point the client at a loopback server.
    YahooFinanceClient(String apiBaseUrl, String cookieSeedUrl) {
        this.apiBaseUrl = apiBaseUrl;
        this.cookieSeedUrl = cookieSeedUrl;
        // The crumb is only valid alongside the session cookie the seed request sets, so the client
        // keeps cookies for its own lifetime. Yahoo sets that cookie with an RFC 1123 Expires
        // ("Sat, 26 Jun 2027 15:47:02 GMT"); a cookie store that rejected the date would drop the
        // cookie and every quoteSummary would 401, which the cookie test reproduces.
        this.httpClient = HttpClient.newBuilder()
                .cookieHandler(new CookieManager(null, CookiePolicy.ACCEPT_ORIGINAL_SERVER))
                .connectTimeout(CONNECT_TIMEOUT)
                .followRedirects(HttpClient.Redirect.NORMAL)
                .build();
    }

    /** Returns the raw quoteSummary JSON for a symbol, refreshing the crumb once on a 401. */
    public String fetchQuoteSummary(String symbol) throws DataRetrievalError {
        try {
            return requestQuoteSummary(symbol);
        } catch (CrumbRejectedException e) {
            log.info("Yahoo rejected the crumb for {}; refreshing and retrying once", symbol);
            crumb = null;
            try {
                return requestQuoteSummary(symbol);
            } catch (CrumbRejectedException retryFailure) {
                throw new DataRetrievalError("Yahoo rejected the crumb twice for " + symbol);
            }
        }
    }

    private String requestQuoteSummary(String symbol) throws DataRetrievalError, CrumbRejectedException {
        String currentCrumb = ensureCrumb();
        String url = apiBaseUrl + "/v10/finance/quoteSummary/" + symbol
                + "?modules=" + MODULES
                + "&crumb=" + URLEncoder.encode(currentCrumb, StandardCharsets.UTF_8);
        HttpResponse<String> response = send(url);
        if (response.statusCode() == 401) {
            throw new CrumbRejectedException();
        }
        if (response.statusCode() != 200) {
            throw new DataRetrievalError("Yahoo quoteSummary returned HTTP " + response.statusCode() + " for " + symbol);
        }
        return response.body();
    }

    private String ensureCrumb() throws DataRetrievalError {
        String current = crumb;
        if (current == null || current.isBlank()) {
            current = fetchCrumb();
            crumb = current;
        }
        return current;
    }

    private String fetchCrumb() throws DataRetrievalError {
        seedCookies();
        HttpResponse<String> response = send(apiBaseUrl + "/v1/test/getcrumb");
        // Yahoo throttles with a 429 whose body is plain text; read as a crumb, it would be sent on,
        // rejected, and reported as a bad crumb instead of the rate limit it is.
        if (response.statusCode() != 200) {
            throw new DataRetrievalError("Yahoo crumb request returned HTTP " + response.statusCode());
        }
        String value = response.body();
        if (value == null || value.isBlank()) {
            throw new DataRetrievalError("Yahoo returned an empty crumb");
        }
        return value.trim();
    }

    private void seedCookies() throws DataRetrievalError {
        try {
            // The seed endpoint often returns a non-200 but still sets the cookie we need.
            httpClient.send(request(cookieSeedUrl), HttpResponse.BodyHandlers.discarding());
        } catch (IOException e) {
            log.warn("Yahoo cookie seed request failed ({}); attempting crumb fetch anyway", e.getMessage());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new DataRetrievalError(e);
        }
    }

    private HttpResponse<String> send(String url) throws DataRetrievalError {
        try {
            return httpClient.send(request(url), HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        } catch (IOException e) {
            throw new DataRetrievalError(e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new DataRetrievalError(e);
        }
    }

    private static HttpRequest request(String url) {
        return HttpRequest.newBuilder(URI.create(url))
                .timeout(REQUEST_TIMEOUT)
                .header("User-Agent", USER_AGENT)
                .GET()
                .build();
    }

    /** Signals that Yahoo rejected the crumb (HTTP 401) so the caller can refresh once. */
    private static final class CrumbRejectedException extends Exception {
    }
}

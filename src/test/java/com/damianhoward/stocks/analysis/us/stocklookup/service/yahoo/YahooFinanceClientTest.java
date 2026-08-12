package com.damianhoward.stocks.analysis.us.stocklookup.service.yahoo;

import com.sun.net.httpserver.HttpServer;
import com.damianhoward.stocks.exception.DataRetrievalError;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Drives YahooFinanceClient against a loopback server that mimics Yahoo's
 * cookie → crumb → quoteSummary flow, so the crumb caching and 401 refresh-and-retry
 * are covered without hitting Yahoo.
 */
class YahooFinanceClientTest {

    private static final String BODY = "{\"quoteSummary\":{\"result\":[{\"price\":{\"longName\":\"Test Co\"}}],\"error\":null}}";

    private HttpServer server;
    private YahooFinanceClient client;
    private final AtomicInteger crumbRequests = new AtomicInteger();
    private final AtomicInteger quoteRequests = new AtomicInteger();
    private final Queue<Integer> quoteStatuses = new ConcurrentLinkedQueue<>();
    private volatile Integer forcedStatus = null;
    private volatile boolean emptyCrumb = false;

    @BeforeEach
    void setUp() throws IOException {
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/seed", exchange -> {
            exchange.getResponseHeaders().add("Set-Cookie", "A1=token; Path=/");
            exchange.sendResponseHeaders(200, -1);
            exchange.close();
        });
        server.createContext("/v1/test/getcrumb", exchange -> {
            if (emptyCrumb) {
                exchange.sendResponseHeaders(200, -1);
                exchange.close();
                return;
            }
            byte[] body = ("crumb-" + crumbRequests.incrementAndGet()).getBytes(StandardCharsets.UTF_8);
            exchange.sendResponseHeaders(200, body.length);
            try (var os = exchange.getResponseBody()) {
                os.write(body);
            }
        });
        server.createContext("/v10/finance/quoteSummary/", exchange -> {
            quoteRequests.incrementAndGet();
            int status = forcedStatus != null ? forcedStatus : (quoteStatuses.isEmpty() ? 200 : quoteStatuses.poll());
            byte[] body = (status == 200 ? BODY : "{\"error\":\"" + status + "\"}").getBytes(StandardCharsets.UTF_8);
            exchange.sendResponseHeaders(status, body.length);
            try (var os = exchange.getResponseBody()) {
                os.write(body);
            }
        });
        server.start();
        String base = "http://127.0.0.1:" + server.getAddress().getPort();
        client = new YahooFinanceClient(base, base + "/seed");
    }

    @AfterEach
    void tearDown() {
        if (server != null) {
            server.stop(0);
        }
    }

    @Test
    void fetchesQuoteSummaryAndCachesTheCrumb() throws DataRetrievalError {
        assertTrue(client.fetchQuoteSummary("AAPL").contains("Test Co"));
        client.fetchQuoteSummary("MSFT");

        assertEquals(1, crumbRequests.get(), "crumb should be fetched once and reused");
        assertEquals(2, quoteRequests.get());
    }

    @Test
    void refreshesCrumbAndRetriesOnceOn401() throws DataRetrievalError {
        quoteStatuses.add(401); // first quoteSummary call is rejected, then default 200

        assertTrue(client.fetchQuoteSummary("AAPL").contains("Test Co"));

        assertEquals(2, crumbRequests.get(), "a 401 should trigger one crumb refresh");
        assertEquals(2, quoteRequests.get(), "the request is retried once after refreshing");
    }

    @Test
    void throwsWhenCrumbRejectedTwice() {
        forcedStatus = 401;
        assertThrows(DataRetrievalError.class, () -> client.fetchQuoteSummary("AAPL"));
    }

    @Test
    void throwsOnNonRetryableServerError() {
        forcedStatus = 500;
        assertThrows(DataRetrievalError.class, () -> client.fetchQuoteSummary("AAPL"));
    }

    @Test
    void throwsWhenCrumbIsEmpty() {
        emptyCrumb = true;
        assertThrows(DataRetrievalError.class, () -> client.fetchQuoteSummary("AAPL"));
    }

    @Test
    void wrapsIoErrorOnQuoteRequest() throws DataRetrievalError {
        // First call succeeds and caches the crumb.
        assertTrue(client.fetchQuoteSummary("AAPL").contains("Test Co"));
        // Server goes away; the cached crumb is reused but the quote request now fails.
        server.stop(0);
        server = null;
        assertThrows(DataRetrievalError.class, () -> client.fetchQuoteSummary("MSFT"));
    }

    @Test
    void wrapsIoErrorWhenServerUnreachable() {
        // Points at a closed port: the cookie seed swallows the IOException and logs,
        // then the crumb fetch fails with an IOException that surfaces as DataRetrievalError.
        YahooFinanceClient unreachable = new YahooFinanceClient("http://127.0.0.1:1", "http://127.0.0.1:1/seed");
        assertThrows(DataRetrievalError.class, () -> unreachable.fetchQuoteSummary("AAPL"));
    }

    @Test
    void defaultConstructorTargetsYahooEndpoints() {
        // The no-arg constructor wires the production URLs and builds the HTTP client
        // without making any network call.
        assertNotNull(new YahooFinanceClient());
    }

    @Test
    void keepsSessionCookieWithRfc1123Expires() throws IOException, DataRetrievalError {
        // Reproduces the real Yahoo gate the old loopback fake missed: the seed sets a
        // session cookie whose Expires uses RFC 1123 spacing ("Sat, 26 Jun 2027 ..."),
        // which HttpClient's default cookie spec rejects and drops. The crumb endpoint
        // only issues the valid crumb when that cookie comes back, and quoteSummary only
        // accepts the valid crumb — so a dropped cookie means a 401 on every request.
        HttpServer cookieServer = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        String validCrumb = "valid-crumb";
        cookieServer.createContext("/seed", exchange -> {
            exchange.getResponseHeaders().add("Set-Cookie",
                    "A3=session-token; Expires=Sat, 26 Jun 2027 15:47:02 GMT; Path=/");
            exchange.sendResponseHeaders(404, -1); // Yahoo's seed 404s but still sets the cookie
            exchange.close();
        });
        cookieServer.createContext("/v1/test/getcrumb", exchange -> {
            String cookie = exchange.getRequestHeaders().getFirst("Cookie");
            boolean hasSession = cookie != null && cookie.contains("A3=session-token");
            byte[] body = (hasSession ? validCrumb : "bad-crumb").getBytes(StandardCharsets.UTF_8);
            exchange.sendResponseHeaders(200, body.length);
            try (var os = exchange.getResponseBody()) {
                os.write(body);
            }
        });
        cookieServer.createContext("/v10/finance/quoteSummary/", exchange -> {
            boolean crumbValid = exchange.getRequestURI().getQuery().contains("crumb=" + validCrumb);
            byte[] body = (crumbValid ? BODY : "{\"error\":\"Invalid Cookie\"}").getBytes(StandardCharsets.UTF_8);
            exchange.sendResponseHeaders(crumbValid ? 200 : 401, body.length);
            try (var os = exchange.getResponseBody()) {
                os.write(body);
            }
        });
        cookieServer.start();
        try {
            String base = "http://127.0.0.1:" + cookieServer.getAddress().getPort();
            YahooFinanceClient cookieClient = new YahooFinanceClient(base, base + "/seed");
            assertTrue(cookieClient.fetchQuoteSummary("AAPL").contains("Test Co"),
                    "session cookie with an RFC 1123 Expires must survive so the crumb validates");
        } finally {
            cookieServer.stop(0);
        }
    }
}

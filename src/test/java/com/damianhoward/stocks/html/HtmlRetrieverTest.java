package com.damianhoward.stocks.html;

import com.sun.net.httpserver.HttpServer;
import com.damianhoward.stocks.exception.DataRetrievalError;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Drives HtmlRetriever against a loopback HttpServer. Retries/backoff are
 * configured to small values so the failure path is exercised without real delays.
 */
class HtmlRetrieverTest {

    private HttpServer server;
    private String baseUrl;
    private final AtomicInteger boomHits = new AtomicInteger();
    private final AtomicInteger notFoundHits = new AtomicInteger();

    @BeforeEach
    void setUp() throws IOException {
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/ok", exchange -> {
            byte[] body = "<html><body>Hello Zacks</body></html>".getBytes(StandardCharsets.UTF_8);
            exchange.sendResponseHeaders(200, body.length);
            try (var os = exchange.getResponseBody()) {
                os.write(body);
            }
        });
        server.createContext("/boom", exchange -> {
            boomHits.incrementAndGet();
            byte[] body = "error".getBytes(StandardCharsets.UTF_8);
            exchange.sendResponseHeaders(500, body.length);
            try (var os = exchange.getResponseBody()) {
                os.write(body);
            }
        });
        server.createContext("/notfound", exchange -> {
            notFoundHits.incrementAndGet();
            byte[] body = "missing".getBytes(StandardCharsets.UTF_8);
            exchange.sendResponseHeaders(404, body.length);
            try (var os = exchange.getResponseBody()) {
                os.write(body);
            }
        });
        server.start();
        baseUrl = "http://127.0.0.1:" + server.getAddress().getPort();
    }

    @AfterEach
    void tearDown() {
        server.stop(0);
    }

    @Test
    void returnsRawAndParsedHtmlOnSuccess() throws DataRetrievalError {
        HtmlResponse response = new HtmlRetriever(5000, 1, 0).getHtml(baseUrl + "/ok");
        assertTrue(response.rawHtml.contains("Hello Zacks"), "raw HTML preserved");
        assertTrue(response.parsedHtml.contains("Hello Zacks"), "Tika-extracted text present");
    }

    @Test
    void retriesTransientServerErrorThenThrows() {
        HtmlRetriever retriever = new HtmlRetriever(5000, 3, 0);
        assertThrows(DataRetrievalError.class, () -> retriever.getHtml(baseUrl + "/boom"));
        assertEquals(3, boomHits.get(), "5xx is transient, so all retries should be used");
    }

    @Test
    void clientErrorFailsFastWithoutRetrying() {
        HtmlRetriever retriever = new HtmlRetriever(5000, 5, 0);
        assertThrows(DataRetrievalError.class, () -> retriever.getHtml(baseUrl + "/notfound"));
        assertEquals(1, notFoundHits.get(), "404 is not transient, so it must not be retried");
    }
}

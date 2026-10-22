package com.damianhoward.stocks.html;

import com.damianhoward.stocks.exception.DataRetrievalError;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

@Component
public class HtmlRetriever {

    private static final Logger log = LoggerFactory.getLogger(HtmlRetriever.class);

    private static final Duration DEFAULT_TIMEOUT = Duration.ofSeconds(30);
    private static final int DEFAULT_MAX_RETRIES = 10;
    private static final long DEFAULT_RETRY_DELAY_MILLIS = 3000;
    private static final String USER_AGENT =
            "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/71.0.3578.98 Safari/537.36";

    private final HttpClient client;
    private final Duration timeout;
    private final int maxRetries;
    private final long retryDelayMillis;

    public HtmlRetriever() {
        this(DEFAULT_TIMEOUT, DEFAULT_MAX_RETRIES, DEFAULT_RETRY_DELAY_MILLIS);
    }

    // Visible for testing: lets a test drive the retry/error path without real backoff delays.
    HtmlRetriever(Duration timeout, int maxRetries, long retryDelayMillis) {
        this.timeout = timeout;
        this.maxRetries = maxRetries;
        this.retryDelayMillis = retryDelayMillis;
        // The JDK client never follows redirects unless told to, unlike the Apache client this
        // replaced. None of today's endpoints redirects, but a moved page should still be read.
        this.client = HttpClient.newBuilder()
                .connectTimeout(timeout)
                .followRedirects(HttpClient.Redirect.NORMAL)
                .build();
    }

    public String getHtml(String url) throws DataRetrievalError {
        HttpRequest request = HttpRequest.newBuilder(URI.create(url))
                .timeout(timeout)
                .header("User-Agent", USER_AGENT)
                .GET()
                .build();

        for (int attempt = 1; attempt <= maxRetries; attempt++) {
            try {
                log.trace("Attempt {} to retrieve html from url: {}", attempt, url);
                HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
                int status = response.statusCode();
                if (status >= 400 && status < 500) {
                    // 4xx client errors (e.g. 404 Not Found) won't change on retry — fail fast.
                    log.error("Not retrying HTTP {} for url: {}", status, url);
                    throw new DataRetrievalError("Unexpected HTTP status " + status + " for " + url);
                }
                if (status < 200 || status >= 300) {
                    throw new IOException("Unexpected HTTP status " + status + " for " + url);
                }
                return response.body();
            } catch (IOException e) {
                if (attempt == maxRetries) {
                    log.error("Giving up retrieving data from url: {}", url, e);
                    throw new DataRetrievalError(e);
                }
                pauseBeforeRetry();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new DataRetrievalError(e);
            }
        }
        throw new DataRetrievalError(new IOException("Unable to retrieve html from " + url));
    }

    private void pauseBeforeRetry() throws DataRetrievalError {
        try {
            Thread.sleep(retryDelayMillis);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new DataRetrievalError(e);
        }
    }
}

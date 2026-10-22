package com.damianhoward.stocks.fx;

import com.damianhoward.stocks.exception.DataRetrievalError;
import com.damianhoward.stocks.html.HtmlRetriever;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.DeserializationFeature;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

import java.math.BigDecimal;
import java.math.MathContext;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class CurrencyConverter {

    private static final Logger log = LoggerFactory.getLogger(CurrencyConverter.class);

    // A rate is read as the decimal Frankfurter wrote rather than the nearest double.
    private static final JsonMapper JSON = JsonMapper.builder()
            .enable(DeserializationFeature.USE_BIG_DECIMAL_FOR_FLOATS)
            .build();

    private final HtmlRetriever htmlRetriever;
    private final String providerUrl;
    // A run converts a handful of currencies for thousands of quotes, so each pair is fetched once.
    private final Map<String, BigDecimal> rates = new ConcurrentHashMap<>();

    public CurrencyConverter(HtmlRetriever htmlRetriever,
                             @Value("${stocks.analysis.us.fx.provider-url:https://api.frankfurter.dev/v2/rates}") String providerUrl) {
        this.htmlRetriever = htmlRetriever;
        this.providerUrl = providerUrl;
    }

    /**
     * Units of {@code to} that one unit of {@code from} buys, or empty when either code is not a
     * three-letter code or the provider quotes no rate for it. A failed fetch is an error rather
     * than an empty answer, so the caller can tell "no such rate" from "could not ask".
     */
    public Optional<BigDecimal> convert(String from, String to) throws DataRetrievalError {
        if (!isCurrencyCode(from) || !isCurrencyCode(to)) {
            log.error("Invalid currency lookup from={} to={}", from, to);
            return Optional.empty();
        }
        BigDecimal cached = rates.get(from + to);
        if (cached != null) {
            return Optional.of(cached);
        }
        // Frankfurter quotes every currency against EUR; ask for both legs and divide.
        String json = htmlRetriever.getHtml(String.format("%s?quotes=%s,%s", providerUrl, from, to));
        Map<String, BigDecimal> perEur = ratesAgainstEur(json);
        BigDecimal fromRate = "EUR".equalsIgnoreCase(from) ? BigDecimal.ONE : perEur.get(from.toUpperCase());
        BigDecimal toRate = "EUR".equalsIgnoreCase(to) ? BigDecimal.ONE : perEur.get(to.toUpperCase());
        if (fromRate == null || toRate == null || fromRate.signum() <= 0 || toRate.signum() <= 0) {
            log.error("FX response missing rate for {} -> {}: {}", from, to, json);
            return Optional.empty();
        }
        BigDecimal conversion = toRate.divide(fromRate, MathContext.DECIMAL64);

        log.info("FX {} -> {} = {}", from, to, conversion);
        rates.put(from + to, conversion);
        return Optional.of(conversion);
    }

    private static boolean isCurrencyCode(String code) {
        return code != null && code.length() == 3;
    }

    /** Frankfurter answers {@code [{"base":"EUR","quote":"GBP","rate":0.86254}, ...]}. */
    private static Map<String, BigDecimal> ratesAgainstEur(String json) throws DataRetrievalError {
        Map<String, BigDecimal> perEur = new HashMap<>();
        try {
            for (JsonNode quote : JSON.readTree(json)) {
                JsonNode rate = quote.get("rate");
                if (rate != null && rate.isNumber()) {
                    perEur.put(quote.path("quote").asString(), rate.decimalValue());
                }
            }
        } catch (JacksonException e) {
            throw new DataRetrievalError(e);
        }
        return perEur;
    }
}

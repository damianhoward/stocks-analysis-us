package io.github.damian1000.stocks.fx;

import io.github.damian1000.stocks.exception.DataRetrievalError;
import io.github.damian1000.stocks.html.HtmlParser;
import io.github.damian1000.stocks.html.HtmlRetriever;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

@Component
public class CurrencyConverter {

    private static final Logger log = LoggerFactory.getLogger(CurrencyConverter.class);

    private final HtmlRetriever htmlRetriever;
    private final HtmlParser htmlParser;
    private final String providerUrl;

    private static final Map<String, Double> rates = new HashMap<>();

    public CurrencyConverter(HtmlRetriever htmlRetriever,
                             HtmlParser htmlParser,
                             @Value("${stocks.analysis.us.fx.provider-url:https://api.frankfurter.dev/v2/rates}") String providerUrl) {
        this.htmlRetriever = htmlRetriever;
        this.htmlParser = htmlParser;
        this.providerUrl = providerUrl;
    }

    public double convert(String from, String to) throws DataRetrievalError {
        if (StringUtils.isEmpty(from) || StringUtils.isEmpty(to) || from.length() != 3 || to.length() != 3) {
            log.error("Invalid currency lookup from={} to={}", from, to);
            return 0.0;
        }
        Double cached = rates.get(from + to);
        if (cached != null) {
            return cached;
        }
        // Frankfurter quotes every currency against EUR; ask for both legs and divide.
        String url = String.format("%s?quotes=%s,%s", providerUrl, from, to);
        String json = htmlRetriever.getHtml(url).rawHtml;

        double fromRate = rateAgainstEur(json, from);
        double toRate = rateAgainstEur(json, to);
        if (fromRate == 0.0 || toRate == 0.0) {
            log.error("FX response missing rate for {} -> {}: {}", from, to, json);
            return 0.0;
        }
        double conversion = toRate / fromRate;

        log.info("FX {} -> {} = {}", from, to, conversion);
        rates.put(from + to, conversion);
        return conversion;
    }

    /** Rate of one EUR in {@code currency}, as quoted by Frankfurter; EUR itself is the base, so 1.0. */
    private double rateAgainstEur(String json, String currency) {
        if ("EUR".equalsIgnoreCase(currency)) {
            return 1.0;
        }
        int index = json.indexOf("\"quote\":\"" + currency.toUpperCase() + "\"");
        if (index < 0) {
            return 0.0;
        }
        String raw = htmlParser.extractText(json.substring(index), "\"rate\":", "}").trim();
        return raw.isEmpty() ? 0.0 : Double.parseDouble(raw);
    }
}

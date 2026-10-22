package com.damianhoward.stocks.analysis.us.sectormapping.service;

import com.damianhoward.stocks.analysis.us.sectormapping.domain.ZacksSectorMapping;
import com.damianhoward.stocks.analysis.us.sectormapping.event.ZacksSectorMappingCompleteEvent;
import com.damianhoward.stocks.analysis.us.sectormapping.event.ZacksSectorMappingStartEvent;
import com.damianhoward.stocks.analysis.us.sectormapping.repository.ZacksSectorMappingRepository;
import com.damianhoward.stocks.exception.DataRetrievalError;
import com.damianhoward.stocks.html.HtmlRetriever;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionTemplate;
import tools.jackson.core.JacksonException;
import tools.jackson.core.JsonParser;
import tools.jackson.core.json.JsonReadFeature;
import tools.jackson.databind.DeserializationFeature;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
public class ZacksSectorMappingService {

    private static final Logger log = LoggerFactory.getLogger(ZacksSectorMappingService.class);

    private static final String APP_DATA_MARKER = "window.app_data =";
    // app_data is a JavaScript object literal in a script tag, not JSON: its first key, columns,
    // is unquoted, and the rest of the page follows it. Those are the two relaxations the page
    // needs, so they are the only two enabled; Jackson 3 rejects trailing content by default.
    private static final JsonMapper APP_DATA = JsonMapper.builder()
            .enable(JsonReadFeature.ALLOW_UNQUOTED_PROPERTY_NAMES)
            .disable(DeserializationFeature.FAIL_ON_TRAILING_TOKENS)
            .build();
    private static final Pattern SPAN_TITLE = Pattern.compile("<span title=\"(.*?)\"");

    private final HtmlRetriever htmlRetriever;
    private final ZacksSectorMappingRepository zacksSectorMappingRepository;
    private final ApplicationEventPublisher eventPublisher;
    private final TransactionTemplate transactionTemplate;

    public ZacksSectorMappingService(
            HtmlRetriever htmlRetriever,
            ZacksSectorMappingRepository zacksSectorMappingRepository,
            ApplicationEventPublisher eventPublisher,
            TransactionTemplate transactionTemplate) {
        this.htmlRetriever = htmlRetriever;
        this.zacksSectorMappingRepository = zacksSectorMappingRepository;
        this.eventPublisher = eventPublisher;
        this.transactionTemplate = transactionTemplate;
    }

    @EventListener
    public void onZacksSectorMappingStartEvent(ZacksSectorMappingStartEvent event) {
        List<ZacksSectorMapping> sectorMapping;
        try {
            sectorMapping = downloadSectorMapping(event.date());
            log.info("Completed retrieving {} sector mapping from zacks", sectorMapping.size());
        } catch (DataRetrievalError dataRetrievalError) {
            log.error("An error occurred while downloading sector mapping", dataRetrievalError);
            throw new IllegalStateException("Unable to download Zacks sector mapping", dataRetrievalError);
        }

        // Swap atomically only after a successful download: a failed download
        // never reaches the delete, and a failed save rolls the delete back.
        transactionTemplate.executeWithoutResult(status -> {
            log.info("Zacks Sector Mapping deleteByDate {}", event.date());
            zacksSectorMappingRepository.deleteByDate(event.date());
            zacksSectorMappingRepository.saveAll(sectorMapping);
        });

        eventPublisher.publishEvent(new ZacksSectorMappingCompleteEvent(event.date()));
    }

    private List<ZacksSectorMapping> downloadSectorMapping(LocalDate date) throws DataRetrievalError {
        log.info("Downloading sector mapping...");
        String page = htmlRetriever.getHtml("https://www.zacks.com/zrank/sector-industry-classification.php");
        log.info("Completed downloading sector mapping.");

        // Without the marker the page has changed shape. Treating that as an empty mapping would
        // delete the date's rows and announce the stage complete.
        int marker = page.indexOf(APP_DATA_MARKER);
        if (marker < 0) {
            throw new DataRetrievalError("Zacks sector page has no " + APP_DATA_MARKER + "; its layout has changed");
        }
        JsonNode appData;
        try (JsonParser parser = APP_DATA.createParser(page.substring(marker + APP_DATA_MARKER.length()))) {
            appData = APP_DATA.readTree(parser);
        } catch (JacksonException e) {
            throw new DataRetrievalError(e);
        }

        List<ZacksSectorMapping> mapping = new ArrayList<>();
        for (JsonNode row : appData.path("data")) {
            mapping.add(ZacksSectorMapping.of(date,
                    spanTitle(row, "Sector Group"),
                    spanTitle(row, "Medium(M) Industry Group"),
                    spanTitle(row, "Expanded(X) Industry Group")));
        }
        if (mapping.isEmpty()) {
            throw new DataRetrievalError("Zacks sector page's app_data has no data rows; its layout has changed");
        }
        // The first row is Zacks's "Index" pseudo-sector, which classifies indices rather than
        // companies, so it is not part of the mapping.
        return mapping.subList(1, mapping.size());
    }

    /** Each cell is an HTML fragment whose span title carries the full, untruncated name. */
    private static String spanTitle(JsonNode row, String field) {
        JsonNode cell = row.get(field);
        if (cell == null || cell.isNull()) {
            return null;
        }
        Matcher m = SPAN_TITLE.matcher(cell.asString());
        return m.find() ? m.group(1) : "could not found anything for " + cell.asString();
    }
}

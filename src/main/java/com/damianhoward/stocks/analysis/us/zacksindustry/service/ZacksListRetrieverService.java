package com.damianhoward.stocks.analysis.us.zacksindustry.service;

import com.damianhoward.stocks.analysis.us.zacksindustry.domain.ZacksList;
import com.damianhoward.stocks.analysis.us.zacksindustry.event.ZacksListCompleteEvent;
import com.damianhoward.stocks.analysis.us.zacksindustry.event.ZacksListStartEvent;
import com.damianhoward.stocks.analysis.us.zacksindustry.repository.ZacksListRepository;
import com.damianhoward.stocks.exception.DataRetrievalError;
import com.damianhoward.stocks.html.HtmlRetriever;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionTemplate;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

@Component
public class ZacksListRetrieverService {

    private static final Logger log = LoggerFactory.getLogger(ZacksListRetrieverService.class);
    private static final JsonMapper JSON = new JsonMapper();

    private final HtmlRetriever htmlRetriever;
    private final ZacksListRepository repository;
    private final ApplicationEventPublisher eventPublisher;
    private final TransactionTemplate transactionTemplate;

    public ZacksListRetrieverService(
            HtmlRetriever htmlRetriever,
            ZacksListRepository repository,
            ApplicationEventPublisher eventPublisher,
            TransactionTemplate transactionTemplate) {
        this.htmlRetriever = htmlRetriever;
        this.repository = repository;
        this.eventPublisher = eventPublisher;
        this.transactionTemplate = transactionTemplate;
    }

    @EventListener
    public void onZacksListStartEvent(ZacksListStartEvent event) {
        log.info("Zacks Industry start");
        List<ZacksList> industries;
        try {
            log.info("Zacks Industry retrieveIndustries");
            industries = retrieveIndustries(event.date());
            log.info("Zacks Industry retrieveIndustries complete");
        } catch (DataRetrievalError dataRetrievalError) {
            log.error("An error occurred while retrieving Zacks industries", dataRetrievalError);
            throw new IllegalStateException("Unable to retrieve Zacks industries", dataRetrievalError);
        }
        // Swap atomically only after a successful fetch: a failed retrieval above
        // never reaches the delete, and a failed save rolls the delete back.
        transactionTemplate.executeWithoutResult(status -> {
            log.info("Zacks Industry deleteByDate {}", event.date());
            repository.deleteByDate(event.date());
            repository.saveAll(industries);
        });
        log.info("Completed Persisting Zacks Industry data");
        eventPublisher.publishEvent(new ZacksListCompleteEvent(event.date()));
    }

    private List<ZacksList> retrieveIndustries(LocalDate date) throws DataRetrievalError {
        String url = "https://www.zacks.com/data_handler/industry/z2_industry_data.php?p=0&t=1";
        JsonNode rows;
        try {
            rows = JSON.readTree(htmlRetriever.getHtml(url)).path("data");
        } catch (JacksonException e) {
            throw new DataRetrievalError(e);
        }
        List<ZacksList> zacksIndustryList = new ArrayList<>();
        for (JsonNode row : rows) {
            zacksIndustryList.add(ZacksList.of(
                    date, scalar(row, "industry_id"), scalar(row, "no_of_stocks"), industryName(row)));
        }
        zacksIndustryList.sort(Comparator.comparingInt(o -> Integer.parseInt(o.index())));
        return zacksIndustryList;
    }

    private static String scalar(JsonNode row, String field) {
        JsonNode value = row.get(field);
        return value == null || !value.isValueNode() || value.isNull() ? null : value.asString();
    }

    /** The name is the text of the first link in an HTML fragment: {@code <a href=...>Banking</a><a ...>(11)</a>}. */
    private static String industryName(JsonNode row) {
        String value = scalar(row, "industry_name");
        if (value == null) {
            return null;
        }
        return value.substring(value.indexOf(">") + 1, value.indexOf("</a>"));
    }

}

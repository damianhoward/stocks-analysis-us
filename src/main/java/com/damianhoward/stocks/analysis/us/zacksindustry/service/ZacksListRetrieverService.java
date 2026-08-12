package com.damianhoward.stocks.analysis.us.zacksindustry.service;

import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonToken;
import com.damianhoward.stocks.analysis.us.zacksindustry.domain.ZacksList;
import com.damianhoward.stocks.analysis.us.zacksindustry.event.ZacksListCompleteEvent;
import com.damianhoward.stocks.analysis.us.zacksindustry.event.ZacksListStartEvent;
import com.damianhoward.stocks.analysis.us.zacksindustry.repository.ZacksListRepository;
import com.damianhoward.stocks.exception.DataRetrievalError;
import com.damianhoward.stocks.html.HtmlRetriever;
import com.damianhoward.stocks.util.IdGenerator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionTemplate;

import java.io.IOException;
import java.io.StringReader;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

@Component
public class ZacksListRetrieverService {

    private static final Logger log = LoggerFactory.getLogger(ZacksListRetrieverService.class);

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
        List<ZacksList> zacksIndustryList = new ArrayList<>();

        String url = "https://www.zacks.com/data_handler/industry/z2_industry_data.php?p=0&t=1";
        String industries = htmlRetriever.getHtml(url).parsedHtml;
        JsonReader reader = new JsonReader(new StringReader(industries));
        reader.setLenient(true);
        try {
            reader.beginObject();
            while (reader.hasNext()) {
                String name = reader.nextName();
                if (name.equals("data")) {
                    // read array
                    reader.beginArray();
                    while (reader.hasNext()) {
                        ZacksList zacksIndustry = new ZacksList();
                        zacksIndustry.setDate(date);
                        zacksIndustry.setId(IdGenerator.generateId());
                        zacksIndustryList.add(zacksIndustry);
                        reader.beginObject();
                        while (reader.hasNext()) {
                            String dataName = reader.nextName();
                            JsonToken token = reader.peek();
                            if (token == JsonToken.BEGIN_ARRAY) {
                                reader.skipValue();
                            } else if (token == JsonToken.BEGIN_OBJECT) {
                                reader.skipValue();
                            } else {
                                if (dataName.equalsIgnoreCase("industry_name")) {
                                    String value = reader.nextString();
                                    int startIndex = value.indexOf(">") + 1;
                                    int endIndex = value.indexOf("</a>");
                                    zacksIndustry.setIndustry(value.substring(startIndex, endIndex));
                                } else if (dataName.equalsIgnoreCase("industry_id")) {
                                    zacksIndustry.setIndex(reader.nextString());
                                } else if (dataName.equalsIgnoreCase("no_of_stocks")) {
                                    zacksIndustry.setTotal(reader.nextString());
                                } else {
                                    reader.skipValue();
                                }
                            }
                        }
                        reader.endObject();
                    }
                    reader.endArray();
                } else {
                    reader.skipValue(); //avoid some unhandle events
                }
            }
            reader.endObject();
            reader.close();
        } catch (IOException e) {
           throw new DataRetrievalError(e);
        }
        zacksIndustryList.sort(Comparator.comparingInt(o -> Integer.valueOf(o.getIndex())));
        return zacksIndustryList;
    }

}

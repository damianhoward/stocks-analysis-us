package com.damianhoward.stocks.analysis.us.sectormapping.service;

import com.google.gson.stream.JsonReader;
import com.damianhoward.stocks.analysis.us.sectormapping.domain.ZacksSectorMapping;
import com.damianhoward.stocks.analysis.us.sectormapping.event.ZacksSectorMappingCompleteEvent;
import com.damianhoward.stocks.analysis.us.sectormapping.event.ZacksSectorMappingStartEvent;
import com.damianhoward.stocks.analysis.us.sectormapping.repository.ZacksSectorMappingRepository;
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
import java.util.Collections;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
public class ZacksSectorMappingService {

    private static final Logger log = LoggerFactory.getLogger(ZacksSectorMappingService.class);

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
        List<ZacksSectorMapping> zacksSectorMappingList = new ArrayList<>();

        log.info("Downloading sector mapping...");
        String url = "https://www.zacks.com/zrank/sector-industry-classification.php";
        String industries = htmlRetriever.getHtml(url).rawHtml;
        log.info("Completed downloading sector mapping.");
        String startWord = "window.app_data =";
        int startIndex = industries.indexOf(startWord)+startWord.length();
        if (industries.length() > startIndex) {
            industries = industries.substring(startIndex).trim();
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
                            reader.beginObject();
                            ZacksSectorMapping zacksSectorMapping = new ZacksSectorMapping();
                            zacksSectorMapping.setId(IdGenerator.generateId());
                            zacksSectorMapping.setDate(date);
                            zacksSectorMappingList.add(zacksSectorMapping);
                            while (reader.hasNext()) {
                                String dataName = reader.nextName();
                                if (dataName.equals("Sector Group")) {
                                    zacksSectorMapping.setSectorGroup(findText(reader.nextString()));
                                } else if (dataName.equals("Medium(M) Industry Group")) {
                                    zacksSectorMapping.setMediumIndustryGroup(findText(reader.nextString()));
                                } else if (dataName.equals("Expanded(X) Industry Group")) {
                                    zacksSectorMapping.setIndustry(findText(reader.nextString()));
                                } else {
                                    reader.skipValue(); //avoid some unhandle events
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

            // remove the header
            zacksSectorMappingList = zacksSectorMappingList.subList(1, zacksSectorMappingList.size());

            return zacksSectorMappingList;
        }
        return Collections.emptyList();
    }

    private String findText(String rawText) {
        // https://docs.oracle.com/javase/7/docs/api/java/util/regex/Pattern.html
        // http://stackoverflow.com/questions/16331423/whats-the-java-regular-expression-for-an-only-integer-numbers-string
        String pattern = "<span title=\"(.*?)\"";
        Pattern r = Pattern.compile(pattern);
        Matcher m = r.matcher(rawText);
        if (m.find()) {
            return m.group(1);
        }
        return "could not found anything for "+rawText;
    }
}

package com.damianhoward.stocks.analysis.us.zackscode.service;

import com.damianhoward.stocks.analysis.us.zackscode.domain.ZacksCode;
import com.damianhoward.stocks.analysis.us.zackscode.event.ZacksBasicCompleteEvent;
import com.damianhoward.stocks.analysis.us.zackscode.event.ZacksBasicStartEvent;
import com.damianhoward.stocks.analysis.us.zackscode.repository.ZacksBasicRepository;
import com.damianhoward.stocks.analysis.us.zacksindustry.domain.ZacksList;
import com.damianhoward.stocks.analysis.us.zacksindustry.repository.ZacksListRepository;
import com.damianhoward.stocks.exception.DataRetrievalError;
import com.damianhoward.stocks.html.HtmlRetriever;
import org.apache.commons.text.WordUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
public class ZacksBasicRetrieverService {

    private static final Logger log = LoggerFactory.getLogger(ZacksBasicRetrieverService.class);

    private static final String INDUSTRY_URL = "https://www.zacks.com/zrank/zacks_industry_rank_data_handler.php?i=";
    private static final String DATA_MARKER = "\"data\"  : [";
    // The page is a JavaScript object literal rather than JSON (unquoted keys, raw newlines in
    // strings), and each symbol sits in an attribute of an HTML fragment, so rows are matched
    // rather than parsed: a company's name, then the first rel attribute after it.
    private static final Pattern COMPANY_ROW = Pattern.compile("\\s+\"Company\"\\s+:\\s+\"(.*?)\".*?rel=\"(.*?)\"");

    private final HtmlRetriever htmlRetriever;
    private final ZacksListRepository zacksIndustryRepository;
    private final ZacksBasicRepository zacksBasicRepository;
    private final ApplicationEventPublisher eventPublisher;
    private final TransactionTemplate transactionTemplate;

    public ZacksBasicRetrieverService(
            HtmlRetriever htmlRetriever,
            ZacksListRepository zacksIndustryRepository,
            ZacksBasicRepository zacksBasicRepository,
            ApplicationEventPublisher eventPublisher,
            TransactionTemplate transactionTemplate) {
        this.htmlRetriever = htmlRetriever;
        this.zacksIndustryRepository = zacksIndustryRepository;
        this.zacksBasicRepository = zacksBasicRepository;
        this.eventPublisher = eventPublisher;
        this.transactionTemplate = transactionTemplate;
    }

    @EventListener
    public void onZacksBasicStartEvent(ZacksBasicStartEvent event) {
        Set<ZacksList> zacksIndustries = zacksIndustryRepository.findByDate(event.date());
        log.info("Number of zacks industries loaded {} for date {}", zacksIndustries.size(), event.date());

        // Fetch every industry's detail up front. If any industry fails we abort
        // here, before deleting the previous run's data.
        List<ZacksCode> zacksCodes = new ArrayList<>();
        zacksIndustries.forEach(industry -> {
            try {
                zacksCodes.addAll(retrieveIndustryDetail(industry, event.date()));
            } catch (DataRetrievalError e) {
                throw new IllegalStateException("Unable to retrieve Zacks basic details for " + industry.industry(), e);
            }
        });

        // Swap atomically only after every industry fetched successfully.
        transactionTemplate.executeWithoutResult(status -> {
            log.info("Zacks Basic deleteByDate {}", event.date());
            zacksBasicRepository.deleteByDate(event.date());
            zacksBasicRepository.saveAll(zacksCodes);
        });
        log.info("Completed retrieving {} basic details from zacks", zacksCodes.size());
        eventPublisher.publishEvent(new ZacksBasicCompleteEvent(event.date()));
    }

    private List<ZacksCode> retrieveIndustryDetail(ZacksList zacksIndustry, LocalDate date) throws DataRetrievalError {
        log.info("Retrieving: {}", zacksIndustry);
        String details = htmlRetriever.getHtml(INDUSTRY_URL + zacksIndustry.index());
        // An industry with no stocks still has the array, empty; without it the page has changed,
        // and reading nothing would quietly drop the industry from the universe.
        int start = details.indexOf(DATA_MARKER);
        if (start < 0) {
            throw new DataRetrievalError("Zacks industry " + zacksIndustry.index() + " has no data array; its layout has changed");
        }
        Matcher row = COMPANY_ROW.matcher(details.substring(start).replace("\\", ""));

        List<ZacksCode> zacksIndustryBasicList = new ArrayList<>();
        while (row.find()) {
            zacksIndustryBasicList.add(ZacksCode.of(
                    date, zacksIndustry.industry(), row.group(2), WordUtils.capitalizeFully(row.group(1))));
        }
        log.info("Number of details for industry: {} is {}", zacksIndustry.industry(), zacksIndustryBasicList.size());
        return zacksIndustryBasicList;
    }

}

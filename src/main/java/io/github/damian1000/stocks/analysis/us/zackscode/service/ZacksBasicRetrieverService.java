package io.github.damian1000.stocks.analysis.us.zackscode.service;

import io.github.damian1000.stocks.analysis.us.zackscode.domain.ZacksCode;
import io.github.damian1000.stocks.analysis.us.zackscode.event.ZacksBasicCompleteEvent;
import io.github.damian1000.stocks.analysis.us.zackscode.event.ZacksBasicStartEvent;
import io.github.damian1000.stocks.analysis.us.zackscode.repository.ZacksBasicRepository;
import io.github.damian1000.stocks.analysis.us.zacksindustry.domain.ZacksList;
import io.github.damian1000.stocks.analysis.us.zacksindustry.repository.ZacksListRepository;
import io.github.damian1000.stocks.exception.DataRetrievalError;
import io.github.damian1000.stocks.html.HtmlParser;
import io.github.damian1000.stocks.html.HtmlRetriever;
import io.github.damian1000.stocks.util.IdGenerator;
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

    private final HtmlRetriever htmlRetriever;
    private final HtmlParser htmlParser;
    private final ZacksListRepository zacksIndustryRepository;
    private final ZacksBasicRepository zacksBasicRepository;
    private final ApplicationEventPublisher eventPublisher;
    private final TransactionTemplate transactionTemplate;

    public ZacksBasicRetrieverService(
            HtmlRetriever htmlRetriever,
            HtmlParser htmlParser,
            ZacksListRepository zacksIndustryRepository,
            ZacksBasicRepository zacksBasicRepository,
            ApplicationEventPublisher eventPublisher,
            TransactionTemplate transactionTemplate) {
        this.htmlRetriever = htmlRetriever;
        this.htmlParser = htmlParser;
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
                throw new IllegalStateException("Unable to retrieve Zacks basic details for " + industry.getIndustry(), e);
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
        List<ZacksCode> zacksIndustryBasicList = new ArrayList<>();
        String index = zacksIndustry.getIndex();
        String url = "https://www.zacks.com/zrank/zacks_industry_rank_data_handler.php?i=";
        log.info("Retrieving: "+zacksIndustry);
        String details = htmlRetriever.getHtml(url+index).parsedHtml;
        String data = htmlParser.extractRow(details, "\"data\"  : [");
        data = data.replace("\\", "");

        // https://docs.oracle.com/javase/7/docs/api/java/util/regex/Pattern.html
        String pattern = "\\s+\"Company\"\\s+:\\s+\"(.*?)\".*?rel=\"(.*?)\"";

        // Create a Pattern object
        Pattern r = Pattern.compile(pattern);

        // Now create matcher object.
        Matcher m = r.matcher(data);

        while (m.find()) {
            String company = m.group(1);
            String symbol = m.group(2);
            ZacksCode basic = new ZacksCode();
            basic.setDate(date);
            basic.setId(IdGenerator.generateId());
            basic.setIndustry(zacksIndustry.getIndustry());
            basic.setCompany(WordUtils.capitalizeFully(company));
            basic.setZacksCode(symbol);
            zacksIndustryBasicList.add(basic);
        }
        log.info("Number of details for industry: "+zacksIndustry.getIndustry() +" is "+zacksIndustryBasicList.size());
        return zacksIndustryBasicList;
    }

}

package com.damianhoward.stocks.analysis.us.analysis.service;

import com.damianhoward.stocks.analysis.us.analysis.domain.AnalysisStock;
import com.damianhoward.stocks.analysis.us.analysis.domain.PEGStock;
import com.damianhoward.stocks.analysis.us.analysis.event.AnalysisStockCompleteEvent;
import com.damianhoward.stocks.analysis.us.analysis.event.AnalysisStockStartEvent;
import com.damianhoward.stocks.analysis.us.analysis.repository.AnalysisRepository;
import com.damianhoward.stocks.analysis.us.sectormapping.domain.ZacksSectorMapping;
import com.damianhoward.stocks.analysis.us.sectormapping.repository.ZacksSectorMappingRepository;
import com.damianhoward.stocks.analysis.us.stocklookup.domain.StockLookup;
import com.damianhoward.stocks.analysis.us.stocklookup.repository.StockLookupRepository;
import com.damianhoward.stocks.analysis.us.zackscode.domain.ZacksCode;
import com.damianhoward.stocks.analysis.us.zackscode.repository.ZacksBasicRepository;
import com.damianhoward.stocks.exception.DataRetrievalError;
import com.damianhoward.stocks.fx.CurrencyConverter;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

@Component
public class AnalysisService {

    private static final Logger log = LoggerFactory.getLogger(AnalysisService.class);

    private final StockLookupRepository stockLookupRepository;
    private final AnalysisRepository analysisRepository;
    private final ZacksBasicRepository zacksBasicRepository;
    private final ZacksSectorMappingRepository zacksSectorMappingRepository;
    private final PEGStockAnalyzer stockAnalyzer;
    private final ApplicationEventPublisher eventPublisher;
    private final CurrencyConverter currencyConverter;

    public AnalysisService(
            StockLookupRepository stockLookupRepository,
            AnalysisRepository analysisRepository,
            ZacksBasicRepository zacksBasicRepository,
            ZacksSectorMappingRepository zacksSectorMappingRepository,
            PEGStockAnalyzer stockAnalyzer,
            ApplicationEventPublisher eventPublisher,
            CurrencyConverter currencyConverter) {
        this.stockLookupRepository = stockLookupRepository;
        this.analysisRepository = analysisRepository;
        this.zacksBasicRepository = zacksBasicRepository;
        this.zacksSectorMappingRepository = zacksSectorMappingRepository;
        this.stockAnalyzer = stockAnalyzer;
        this.eventPublisher = eventPublisher;
        this.currencyConverter = currencyConverter;
    }

    @EventListener
    @Transactional
    public void onAnalysisServiceEvent(AnalysisStockStartEvent event) {
        log.info("Retrieving stock lookup for date {}", event.date());
        Set<StockLookup> stockLookupList = stockLookupRepository.findByDate(event.date());
        log.info("Retrieved a total of stock lookup {} for date {}", stockLookupList.size(), event.date());

        log.info("Deleting analysis for date {}", event.date());
        analysisRepository.deleteByDate(event.date());

        String zacksDateString = System.getProperty("zacksDate");
        LocalDate zacksDate;
        if (zacksDateString != null) {
            zacksDate = LocalDate.parse(zacksDateString);
        } else {
            zacksDate = event.date();
        }
        log.info("Retrieving zacks basic for date {}", zacksDate);

        Set<ZacksCode> zacksCodeSet = zacksBasicRepository.findByDate(zacksDate);
        Map<String, ZacksCode> zacksBasicMap = zacksCodeSet.stream().collect(
                Collectors.toMap(ZacksCode::zacksCode, Function.identity()));

        log.info("Retrieving zacks sector mapping for date {}",zacksDate);
        List<ZacksSectorMapping> zacksSectorMappingList = zacksSectorMappingRepository.findByDate(zacksDate);
        Map<String, ZacksSectorMapping> zacksSectorMappingMap = zacksSectorMappingList.stream().collect(
                Collectors.toMap(zacksSectorMapping -> zacksSectorMapping.industry().toUpperCase(), Function.identity()));

        List<AnalysisStock> analysisStocks = stockLookupList.stream().map(rawLookup -> {
            // Normalise to USD at the analysis boundary; every field read below is then in USD.
            StockLookup stockLookup = toUsd(rawLookup);

            ZacksCode listing = zacksBasicMap.get(stockLookup.zacksCode());
            ZacksSectorMapping sector = null;
            if (listing == null) {
                log.error("Could not find basic zacks for {}", stockLookup.zacksCode());
            } else {
                sector = zacksSectorMappingMap.get(listing.industry().toUpperCase());
                if (sector == null) {
                    log.error("Could not find sector mapping for {}", listing.industry());
                }
            }

            PEGStock pegStock = stockAnalyzer.analyzeStocks(stockLookup);
            return AnalysisStock.of(event.date(), stockLookup, listing, sector, pegStock);
        }).toList();

        log.info("Persisting {} number of analysis stock", analysisStocks.size());
        analysisRepository.saveAll(analysisStocks);
        log.info("Complete persisting {} number of analysis stock", analysisStocks.size());

        eventPublisher.publishEvent(new AnalysisStockCompleteEvent(event.date()));
    }

    /** Normalises a scraped lookup to USD so every downstream stage (calc + export) works in one currency. */
    private StockLookup toUsd(StockLookup lookup) {
        double rate = usdRate(lookup.quote().currency());
        if (rate <= 0.0 || rate == 1.0) {
            // Already USD, blank/unknown currency, or no rate available -> keep the native values.
            return lookup;
        }
        return lookup.withQuote(lookup.quote().convertedTo("USD", rate));
    }

    private double usdRate(String currency) {
        if (StringUtils.isBlank(currency) || "USD".equalsIgnoreCase(currency)) {
            return 1.0;
        }
        try {
            return currencyConverter.convert(currency, "USD");
        } catch (DataRetrievalError e) {
            log.warn("FX {} -> USD failed; retaining native values", currency, e);
            return 0.0;
        }
    }

}

package com.damianhoward.stocks.analysis.us.analysis.service;

import com.damianhoward.stocks.analysis.us.sectormapping.domain.ZacksSectorMapping;
import com.damianhoward.stocks.analysis.us.sectormapping.repository.ZacksSectorMappingRepository;
import com.damianhoward.stocks.analysis.us.zackscode.domain.ZacksCode;
import com.damianhoward.stocks.analysis.us.zackscode.repository.ZacksBasicRepository;
import com.damianhoward.stocks.analysis.us.stocklookup.domain.StockLookup;
import com.damianhoward.stocks.analysis.us.stocklookup.repository.StockLookupRepository;
import com.damianhoward.stocks.analysis.us.analysis.domain.AnalysisStock;
import com.damianhoward.stocks.analysis.us.analysis.domain.PEGStock;
import com.damianhoward.stocks.analysis.us.analysis.domain.PegRatios;
import com.damianhoward.stocks.analysis.us.analysis.event.AnalysisStockCompleteEvent;
import com.damianhoward.stocks.analysis.us.analysis.event.AnalysisStockStartEvent;
import com.damianhoward.stocks.analysis.us.analysis.repository.AnalysisRepository;
import com.damianhoward.stocks.exception.DataRetrievalError;
import com.damianhoward.stocks.fx.CurrencyConverter;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.context.ApplicationEventPublisher;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Set;

import static com.damianhoward.stocks.analysis.us.stocklookup.domain.QuoteBuilder.aQuote;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AnalysisServiceTest {

    private StockLookupRepository stockLookupRepository;
    private AnalysisRepository analysisRepository;
    private ZacksBasicRepository zacksBasicRepository;
    private ZacksSectorMappingRepository zacksSectorMappingRepository;
    private PEGStockAnalyzer pegStockAnalyzer;
    private ApplicationEventPublisher eventPublisher;
    private CurrencyConverter currencyConverter;
    private AnalysisService service;

    @BeforeEach
    void setUp() {
        stockLookupRepository = mock(StockLookupRepository.class);
        analysisRepository = mock(AnalysisRepository.class);
        zacksBasicRepository = mock(ZacksBasicRepository.class);
        zacksSectorMappingRepository = mock(ZacksSectorMappingRepository.class);
        pegStockAnalyzer = mock(PEGStockAnalyzer.class);
        eventPublisher = mock(ApplicationEventPublisher.class);
        currencyConverter = mock(CurrencyConverter.class);

        service = new AnalysisService(
                stockLookupRepository,
                analysisRepository,
                zacksBasicRepository,
                zacksSectorMappingRepository,
                pegStockAnalyzer,
                eventPublisher,
                currencyConverter);
    }

    @AfterEach
    void clearSystemProperty() {
        System.clearProperty("zacksDate");
    }

    @Test
    void joinsLookupZacksAndSectorMappingAndPersistsAnalysisStocks() {
        LocalDate date = LocalDate.of(2024, 5, 1);
        StockLookup lookup = StockLookup.quoted(date, "ZC1", aQuote().company("Acme")
                .currency("USD").marketCap(BigDecimal.TEN).yearEnding("12")
                .price(BigDecimal.valueOf(50)).lastYearEPS(BigDecimal.ONE)
                .thisYearEstimateEPS(BigDecimal.valueOf(2))
                .nextYearEstimateEPS(BigDecimal.valueOf(3)).build());
        when(stockLookupRepository.findByDate(date)).thenReturn(Set.of(lookup));

        ZacksCode zacksCode = ZacksCode.of(date, "Software", "ZC1", "Acme Zacks Co");
        when(zacksBasicRepository.findByDate(date)).thenReturn(Set.of(zacksCode));

        ZacksSectorMapping mapping = ZacksSectorMapping.of(date, "Tech", "Apps", "Software");
        when(zacksSectorMappingRepository.findByDate(date)).thenReturn(List.of(mapping));

        when(pegStockAnalyzer.analyzeStocks(lookup)).thenReturn(new PEGStock(
                new PegRatios(
                        BigDecimal.valueOf(25),
                        BigDecimal.valueOf(16.67),
                        BigDecimal.valueOf(50),
                        BigDecimal.valueOf(33.33),
                        BigDecimal.valueOf(0.5),
                        BigDecimal.valueOf(0.5)),
                "00 Good"));

        service.onAnalysisServiceEvent(new AnalysisStockStartEvent(date));

        verify(analysisRepository).deleteByDate(date);
        ArgumentCaptor<List<AnalysisStock>> captor = ArgumentCaptor.forClass(List.class);
        verify(analysisRepository).saveAll(captor.capture());
        List<AnalysisStock> persisted = captor.getValue();
        assertEquals(1, persisted.size());
        AnalysisStock built = persisted.get(0);
        assertEquals("Tech", built.sectorGroup());
        assertEquals("Apps", built.mediumIndustryGroup());
        assertEquals("Software", built.industry());
        assertEquals("Acme Zacks Co", built.zacksCompany());
        assertEquals("00 Good", built.category());
        verify(eventPublisher).publishEvent(any(AnalysisStockCompleteEvent.class));
    }

    @Test
    void missingZacksAndSectorMappingPersistsAStockWithoutThoseFields() {
        LocalDate date = LocalDate.of(2024, 5, 1);
        StockLookup lookup = StockLookup.quoted(date, "UNKNOWN", aQuote().company("Mystery").build());
        when(stockLookupRepository.findByDate(date)).thenReturn(Set.of(lookup));
        when(zacksBasicRepository.findByDate(date)).thenReturn(Set.of()); // no zacks
        when(zacksSectorMappingRepository.findByDate(date)).thenReturn(List.of());
        when(pegStockAnalyzer.analyzeStocks(lookup)).thenReturn(PEGStock.categorised("20 Reuters Lookup Invalid"));

        service.onAnalysisServiceEvent(new AnalysisStockStartEvent(date));

        ArgumentCaptor<List<AnalysisStock>> captor = ArgumentCaptor.forClass(List.class);
        verify(analysisRepository).saveAll(captor.capture());
        AnalysisStock built = captor.getValue().get(0);
        // No zacks/sector found -> these stay null but the row still gets saved
        assertNull(built.sectorGroup());
        assertNull(built.zacksCompany());
        assertEquals("20 Reuters Lookup Invalid", built.category());
    }

    @Test
    void zacksDateSystemPropertyOverridesEventDate() {
        LocalDate eventDate = LocalDate.of(2024, 5, 1);
        LocalDate zacksDate = LocalDate.of(2024, 4, 1);
        System.setProperty("zacksDate", zacksDate.toString());

        when(stockLookupRepository.findByDate(eventDate)).thenReturn(Set.of());
        when(zacksBasicRepository.findByDate(zacksDate)).thenReturn(Set.of());
        when(zacksSectorMappingRepository.findByDate(zacksDate)).thenReturn(List.of());

        service.onAnalysisServiceEvent(new AnalysisStockStartEvent(eventDate));

        verify(zacksBasicRepository).findByDate(zacksDate);
        verify(zacksSectorMappingRepository).findByDate(zacksDate);
    }

    @Test
    void normalisesForeignCurrencyValuesToUsd() throws DataRetrievalError {
        LocalDate date = LocalDate.of(2024, 5, 1);
        // targetPrice deliberately absent, which exercises the null passthrough.
        StockLookup lookup = StockLookup.quoted(date, "GB1", aQuote().company("Britannia")
                .currency("GBP").marketCap(BigDecimal.valueOf(100))
                .price(BigDecimal.valueOf(40)).lastYearEPS(BigDecimal.valueOf(2))
                .thisYearEstimateEPS(BigDecimal.valueOf(4)).nextYearEstimateEPS(BigDecimal.valueOf(5)).build());
        when(stockLookupRepository.findByDate(date)).thenReturn(Set.of(lookup));
        when(zacksBasicRepository.findByDate(date)).thenReturn(Set.of());
        when(zacksSectorMappingRepository.findByDate(date)).thenReturn(List.of());
        when(currencyConverter.convert("GBP", "USD")).thenReturn(1.25);
        when(pegStockAnalyzer.analyzeStocks(any())).thenReturn(PEGStock.categorised("00 Good"));

        service.onAnalysisServiceEvent(new AnalysisStockStartEvent(date));

        ArgumentCaptor<List<AnalysisStock>> captor = ArgumentCaptor.forClass(List.class);
        verify(analysisRepository).saveAll(captor.capture());
        AnalysisStock built = captor.getValue().get(0);
        assertEquals("USD", built.quote().currency());
        assertEquals(0, BigDecimal.valueOf(50).compareTo(built.quote().price()));        // 40 * 1.25
        assertEquals(0, BigDecimal.valueOf(125).compareTo(built.quote().marketCap()));   // 100 * 1.25
        assertNull(built.quote().targetPrice());        // null stays null
    }

    @Test
    void retainsNativeValuesWhenNoFxRateAvailable() throws DataRetrievalError {
        LocalDate date = LocalDate.of(2024, 5, 1);
        StockLookup lookup = StockLookup.quoted(date, "EU1", aQuote().company("Europa")
                .currency("EUR").price(BigDecimal.valueOf(30)).build());
        when(stockLookupRepository.findByDate(date)).thenReturn(Set.of(lookup));
        when(zacksBasicRepository.findByDate(date)).thenReturn(Set.of());
        when(zacksSectorMappingRepository.findByDate(date)).thenReturn(List.of());
        when(currencyConverter.convert("EUR", "USD")).thenReturn(0.0); // no rate
        when(pegStockAnalyzer.analyzeStocks(any())).thenReturn(PEGStock.categorised("00 Good"));

        service.onAnalysisServiceEvent(new AnalysisStockStartEvent(date));

        ArgumentCaptor<List<AnalysisStock>> captor = ArgumentCaptor.forClass(List.class);
        verify(analysisRepository).saveAll(captor.capture());
        AnalysisStock built = captor.getValue().get(0);
        assertEquals("EUR", built.quote().currency());
        assertEquals(0, BigDecimal.valueOf(30).compareTo(built.quote().price()));
    }

    @Test
    void retainsNativeValuesWhenFxLookupFails() throws DataRetrievalError {
        LocalDate date = LocalDate.of(2024, 5, 1);
        StockLookup lookup = StockLookup.quoted(date, "JP1", aQuote().company("Nihon")
                .currency("JPY").price(BigDecimal.valueOf(1000)).build());
        when(stockLookupRepository.findByDate(date)).thenReturn(Set.of(lookup));
        when(zacksBasicRepository.findByDate(date)).thenReturn(Set.of());
        when(zacksSectorMappingRepository.findByDate(date)).thenReturn(List.of());
        when(currencyConverter.convert("JPY", "USD")).thenThrow(new DataRetrievalError("broker down"));
        when(pegStockAnalyzer.analyzeStocks(any())).thenReturn(PEGStock.categorised("00 Good"));

        service.onAnalysisServiceEvent(new AnalysisStockStartEvent(date));

        ArgumentCaptor<List<AnalysisStock>> captor = ArgumentCaptor.forClass(List.class);
        verify(analysisRepository).saveAll(captor.capture());
        AnalysisStock built = captor.getValue().get(0);
        assertEquals("JPY", built.quote().currency());
        assertEquals(0, BigDecimal.valueOf(1000).compareTo(built.quote().price()));
    }
}

package io.github.damian1000.stocks.analysis.us.analysis.service;

import io.github.damian1000.stocks.analysis.us.sectormapping.domain.ZacksSectorMapping;
import io.github.damian1000.stocks.analysis.us.sectormapping.repository.ZacksSectorMappingRepository;
import io.github.damian1000.stocks.analysis.us.zackscode.domain.ZacksCode;
import io.github.damian1000.stocks.analysis.us.zackscode.repository.ZacksBasicRepository;
import io.github.damian1000.stocks.analysis.us.stocklookup.domain.StockLookup;
import io.github.damian1000.stocks.analysis.us.stocklookup.repository.StockLookupRepository;
import io.github.damian1000.stocks.analysis.us.analysis.domain.AnalysisStock;
import io.github.damian1000.stocks.analysis.us.analysis.domain.PEGStock;
import io.github.damian1000.stocks.analysis.us.analysis.event.AnalysisStockCompleteEvent;
import io.github.damian1000.stocks.analysis.us.analysis.event.AnalysisStockStartEvent;
import io.github.damian1000.stocks.analysis.us.analysis.repository.AnalysisRepository;
import io.github.damian1000.stocks.exception.DataRetrievalError;
import io.github.damian1000.stocks.fx.CurrencyConverter;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.context.ApplicationEventPublisher;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
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
        StockLookup lookup = StockLookup.builder()
                .id("L1").date(date).zacksCode("ZC1").company("Acme")
                .currency("USD").marketCap(BigDecimal.TEN).yearEnding("12")
                .price(BigDecimal.valueOf(50)).lastYearEPS(BigDecimal.ONE)
                .thisYearEstimateEPS(BigDecimal.valueOf(2))
                .nextYearEstimateEPS(BigDecimal.valueOf(3))
                .build();
        when(stockLookupRepository.findByDate(date)).thenReturn(Set.of(lookup));

        ZacksCode zacksCode = new ZacksCode();
        zacksCode.setZacksCode("ZC1");
        zacksCode.setCompany("Acme Zacks Co");
        zacksCode.setIndustry("Software");
        when(zacksBasicRepository.findByDate(date)).thenReturn(Set.of(zacksCode));

        ZacksSectorMapping mapping = new ZacksSectorMapping();
        mapping.setIndustry("Software");
        mapping.setSectorGroup("Tech");
        mapping.setMediumIndustryGroup("Apps");
        when(zacksSectorMappingRepository.findByDate(date)).thenReturn(List.of(mapping));

        when(pegStockAnalyzer.analyzeStocks(lookup)).thenReturn(new PEGStock(
                "ZC1",
                BigDecimal.valueOf(25),
                BigDecimal.valueOf(16.67),
                BigDecimal.valueOf(50),
                BigDecimal.valueOf(33.33),
                BigDecimal.valueOf(0.5),
                BigDecimal.valueOf(0.5),
                "00 Good"));

        service.onAnalysisServiceEvent(new AnalysisStockStartEvent(date));

        verify(analysisRepository).deleteByDate(date);
        ArgumentCaptor<List<AnalysisStock>> captor = ArgumentCaptor.forClass(List.class);
        verify(analysisRepository).saveAll(captor.capture());
        List<AnalysisStock> persisted = captor.getValue();
        assertEquals(1, persisted.size());
        AnalysisStock built = persisted.get(0);
        assertEquals("Tech", built.getSectorGroup());
        assertEquals("Apps", built.getMediumIndustryGroup());
        assertEquals("Software", built.getIndustry());
        assertEquals("Acme Zacks Co", built.getZacksCompany());
        assertEquals("00 Good", built.getCategory());
        verify(eventPublisher).publishEvent(any(AnalysisStockCompleteEvent.class));
    }

    @Test
    void missingZacksAndSectorMappingPersistsAStockWithoutThoseFields() {
        LocalDate date = LocalDate.of(2024, 5, 1);
        StockLookup lookup = StockLookup.builder()
                .id("L2").date(date).zacksCode("UNKNOWN").company("Mystery").build();
        when(stockLookupRepository.findByDate(date)).thenReturn(Set.of(lookup));
        when(zacksBasicRepository.findByDate(date)).thenReturn(Set.of()); // no zacks
        when(zacksSectorMappingRepository.findByDate(date)).thenReturn(List.of());
        when(pegStockAnalyzer.analyzeStocks(lookup)).thenReturn(PEGStock.categorised("20 Reuters Lookup Invalid"));

        service.onAnalysisServiceEvent(new AnalysisStockStartEvent(date));

        ArgumentCaptor<List<AnalysisStock>> captor = ArgumentCaptor.forClass(List.class);
        verify(analysisRepository).saveAll(captor.capture());
        AnalysisStock built = captor.getValue().get(0);
        // No zacks/sector found -> these stay null but the row still gets saved
        org.junit.jupiter.api.Assertions.assertNull(built.getSectorGroup());
        org.junit.jupiter.api.Assertions.assertNull(built.getZacksCompany());
        assertEquals("20 Reuters Lookup Invalid", built.getCategory());
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
        StockLookup lookup = StockLookup.builder()
                .id("L3").date(date).zacksCode("GB1").company("Britannia")
                .currency("GBP").marketCap(BigDecimal.valueOf(100))
                .price(BigDecimal.valueOf(40)).lastYearEPS(BigDecimal.valueOf(2))
                .thisYearEstimateEPS(BigDecimal.valueOf(4)).nextYearEstimateEPS(BigDecimal.valueOf(5))
                .build(); // targetPrice deliberately null -> exercises the null-passthrough
        when(stockLookupRepository.findByDate(date)).thenReturn(Set.of(lookup));
        when(zacksBasicRepository.findByDate(date)).thenReturn(Set.of());
        when(zacksSectorMappingRepository.findByDate(date)).thenReturn(List.of());
        when(currencyConverter.convert("GBP", "USD")).thenReturn(1.25);
        when(pegStockAnalyzer.analyzeStocks(any())).thenReturn(PEGStock.categorised("00 Good"));

        service.onAnalysisServiceEvent(new AnalysisStockStartEvent(date));

        ArgumentCaptor<List<AnalysisStock>> captor = ArgumentCaptor.forClass(List.class);
        verify(analysisRepository).saveAll(captor.capture());
        AnalysisStock built = captor.getValue().get(0);
        assertEquals("USD", built.getCurrency());
        assertEquals(0, BigDecimal.valueOf(50).compareTo(built.getPrice()));        // 40 * 1.25
        assertEquals(0, BigDecimal.valueOf(125).compareTo(built.getMarketCap()));   // 100 * 1.25
        org.junit.jupiter.api.Assertions.assertNull(built.getTargetPrice());        // null stays null
    }

    @Test
    void retainsNativeValuesWhenNoFxRateAvailable() throws DataRetrievalError {
        LocalDate date = LocalDate.of(2024, 5, 1);
        StockLookup lookup = StockLookup.builder()
                .id("L4").date(date).zacksCode("EU1").company("Europa")
                .currency("EUR").price(BigDecimal.valueOf(30)).build();
        when(stockLookupRepository.findByDate(date)).thenReturn(Set.of(lookup));
        when(zacksBasicRepository.findByDate(date)).thenReturn(Set.of());
        when(zacksSectorMappingRepository.findByDate(date)).thenReturn(List.of());
        when(currencyConverter.convert("EUR", "USD")).thenReturn(0.0); // no rate
        when(pegStockAnalyzer.analyzeStocks(any())).thenReturn(PEGStock.categorised("00 Good"));

        service.onAnalysisServiceEvent(new AnalysisStockStartEvent(date));

        ArgumentCaptor<List<AnalysisStock>> captor = ArgumentCaptor.forClass(List.class);
        verify(analysisRepository).saveAll(captor.capture());
        AnalysisStock built = captor.getValue().get(0);
        assertEquals("EUR", built.getCurrency());
        assertEquals(0, BigDecimal.valueOf(30).compareTo(built.getPrice()));
    }

    @Test
    void retainsNativeValuesWhenFxLookupFails() throws DataRetrievalError {
        LocalDate date = LocalDate.of(2024, 5, 1);
        StockLookup lookup = StockLookup.builder()
                .id("L5").date(date).zacksCode("JP1").company("Nihon")
                .currency("JPY").price(BigDecimal.valueOf(1000)).build();
        when(stockLookupRepository.findByDate(date)).thenReturn(Set.of(lookup));
        when(zacksBasicRepository.findByDate(date)).thenReturn(Set.of());
        when(zacksSectorMappingRepository.findByDate(date)).thenReturn(List.of());
        when(currencyConverter.convert("JPY", "USD")).thenThrow(new DataRetrievalError("broker down"));
        when(pegStockAnalyzer.analyzeStocks(any())).thenReturn(PEGStock.categorised("00 Good"));

        service.onAnalysisServiceEvent(new AnalysisStockStartEvent(date));

        ArgumentCaptor<List<AnalysisStock>> captor = ArgumentCaptor.forClass(List.class);
        verify(analysisRepository).saveAll(captor.capture());
        AnalysisStock built = captor.getValue().get(0);
        assertEquals("JPY", built.getCurrency());
        assertEquals(0, BigDecimal.valueOf(1000).compareTo(built.getPrice()));
    }
}

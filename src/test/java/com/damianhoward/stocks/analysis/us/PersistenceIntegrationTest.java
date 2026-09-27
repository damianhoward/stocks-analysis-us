package com.damianhoward.stocks.analysis.us;

import com.damianhoward.stocks.analysis.us.analysis.domain.AnalysisStock;
import com.damianhoward.stocks.analysis.us.analysis.domain.PEGStock;
import com.damianhoward.stocks.analysis.us.analysis.domain.PegRatios;
import com.damianhoward.stocks.analysis.us.analysis.repository.AnalysisRepository;
import com.damianhoward.stocks.analysis.us.sectormapping.domain.ZacksSectorMapping;
import com.damianhoward.stocks.analysis.us.sectormapping.repository.ZacksSectorMappingRepository;
import com.damianhoward.stocks.analysis.us.stocklookup.domain.StockLookup;
import com.damianhoward.stocks.analysis.us.stocklookup.repository.StockLookupRepository;
import com.damianhoward.stocks.analysis.us.zackscode.domain.ZacksCode;
import com.damianhoward.stocks.analysis.us.zackscode.repository.ZacksBasicRepository;
import com.damianhoward.stocks.analysis.us.zacksindustry.domain.ZacksList;
import com.damianhoward.stocks.analysis.us.zacksindustry.repository.ZacksListRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static com.damianhoward.stocks.analysis.us.stocklookup.domain.QuoteBuilder.aQuote;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Every record against the schema Flyway builds in PostgreSQL. The column names live in
 * annotations and the migrations separately, so a mismatch surfaces only against the real
 * database: a row written and read back equal is the proof that each field lands in its column.
 *
 * <p>Each test rolls back, so every one starts from an empty schema.
 */
@Testcontainers
@SpringBootTest
@ActiveProfiles("integration")
@Transactional
class PersistenceIntegrationTest {

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:18.6-alpine");

    private static final LocalDate DAY = LocalDate.of(2024, 6, 3);
    private static final LocalDate PREVIOUS_DAY = DAY.minusDays(1);

    @Autowired
    private ZacksListRepository industries;

    @Autowired
    private ZacksBasicRepository listings;

    @Autowired
    private ZacksSectorMappingRepository sectors;

    @Autowired
    private StockLookupRepository lookups;

    @Autowired
    private AnalysisRepository analyses;

    @Test
    void zacksIndustriesRoundTrip() {
        List<ZacksList> saved = industries.saveAll(List.of(
                ZacksList.of(DAY, "12", "40", "Software"), ZacksList.of(DAY, "13", "7", "Banks")));

        assertEquals(new HashSet<>(saved), industries.findByDate(DAY));
    }

    @Test
    void zacksListingsRoundTrip() {
        List<ZacksCode> saved = listings.saveAll(List.of(ZacksCode.of(DAY, "Software", "ACME", "Acme Corp")));

        assertEquals(new HashSet<>(saved), listings.findByDate(DAY));
    }

    @Test
    void sectorMappingsRoundTrip() {
        List<ZacksSectorMapping> saved =
                sectors.saveAll(List.of(ZacksSectorMapping.of(DAY, "Tech", "Apps", "Software")));

        assertEquals(saved, sectors.findByDate(DAY));
    }

    @Test
    void aQuotedLookupRoundTripsEveryQuoteField() {
        StockLookup quoted = StockLookup.quoted(DAY, "ACME", aQuote()
                .company("Acme Corp")
                .currency("USD")
                .marketCap(new BigDecimal("3000000000000"))
                .yearEnding("Dec")
                .beta(new BigDecimal("1.2"))
                .price(new BigDecimal("180.50"))
                .targetPrice(new BigDecimal("200.00"))
                .lastYearEPS(new BigDecimal("4.10"))
                .lastYearPE(new BigDecimal("28.5"))
                .thisYearEstimateEPS(new BigDecimal("5.25"))
                .nextYearEstimateEPS(new BigDecimal("6.00"))
                .earningAboveEstimates("3 out of 4 above estimated eps")
                .recommendationRating(new BigDecimal("2.1"))
                .build());

        StockLookup saved = lookups.save(quoted);

        assertEquals(Set.of(saved), lookups.findByDate(DAY));
    }

    @Test
    void aFailedLookupRoundTripsWithAnEmptyQuote() {
        StockLookup saved = lookups.save(StockLookup.failed(DAY, "GONE", "no quoteSummary result"));

        StockLookup found = lookups.findByDate(DAY).iterator().next();
        assertEquals(saved, found);
        assertEquals("no quoteSummary result", found.errorMessage());
    }

    @Test
    void anAnalysisRowRoundTripsWithItsRatios() {
        StockLookup lookup = StockLookup.quoted(
                DAY, "ACME", aQuote().company("Acme Corp").price(new BigDecimal("180.50")).build());
        PegRatios ratios = new PegRatios(
                new BigDecimal("25.1000"),
                new BigDecimal("20.2000"),
                new BigDecimal("15.3000"),
                new BigDecimal("12.4000"),
                new BigDecimal("1.6400"),
                new BigDecimal("1.4200"));
        AnalysisStock row = AnalysisStock.of(
                DAY,
                lookup,
                ZacksCode.of(DAY, "Software", "ACME", "Acme Zacks Co"),
                ZacksSectorMapping.of(DAY, "Tech", "Apps", "Software"),
                new PEGStock(ratios, "00 Good"));

        AnalysisStock saved = analyses.save(row);

        assertEquals(Set.of(saved), analyses.findByDate(DAY));
    }

    @Test
    void theDatabaseAssignsIds() {
        List<StockLookup> saved = lookups.saveAll(List.of(
                StockLookup.failed(DAY, "A", "x"), StockLookup.failed(DAY, "B", "y")));
        List<AnalysisStock> rows = analyses.saveAll(List.of(analysisRow("A"), analysisRow("B")));

        assertNotNull(saved.get(0).id());
        assertNotEquals(saved.get(0).id(), saved.get(1).id());
        assertTrue(rows.get(1).id() > rows.get(0).id(), "analysis ids come from the sequence in order");
    }

    @Test
    void deleteByDateRemovesOnlyThatDay() {
        analyses.saveAll(List.of(analysisRow("A"), analysisRow("B")));
        analyses.save(AnalysisStock.of(
                PREVIOUS_DAY, StockLookup.failed(PREVIOUS_DAY, "C", "x"), null, null, PEGStock.categorised("c")));

        analyses.deleteByDate(DAY);

        assertEquals(0, analyses.findByDate(DAY).size());
        assertEquals(1, analyses.findByDate(PREVIOUS_DAY).size());
    }

    private static AnalysisStock analysisRow(String zacksCode) {
        return AnalysisStock.of(DAY, StockLookup.failed(DAY, zacksCode, "x"), null, null, PEGStock.categorised("c"));
    }
}

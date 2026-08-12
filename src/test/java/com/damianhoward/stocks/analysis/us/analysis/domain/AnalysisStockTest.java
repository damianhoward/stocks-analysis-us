package com.damianhoward.stocks.analysis.us.analysis.domain;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class AnalysisStockTest {

    @Test
    void builderAndAccessorsRoundTrip() {
        AnalysisStock s = AnalysisStock.builder()
                .id(7L)
                .date(LocalDate.of(2024, 1, 2))
                .company("Acme Corp")
                .currency("USD")
                .marketCap(BigDecimal.valueOf(1_000_000))
                .price(BigDecimal.valueOf(100))
                .nextYearPEG(BigDecimal.valueOf(1.1))
                .category("A")
                .build();
        assertEquals(7L, s.getId());
        assertEquals("Acme Corp", s.getCompany());
        assertEquals(BigDecimal.valueOf(1_000_000), s.getMarketCap());
        assertNotNull(new AnalysisStock()); // @NoArgsConstructor used by JPA
    }

    @Test
    void compareToOrdersByCategoryThenByNextYearPegDescending() {
        AnalysisStock a = AnalysisStock.builder().category("A").nextYearPEG(BigDecimal.valueOf(1.0)).build();
        AnalysisStock aHigh = AnalysisStock.builder().category("A").nextYearPEG(BigDecimal.valueOf(2.0)).build();
        AnalysisStock b = AnalysisStock.builder().category("B").nextYearPEG(BigDecimal.valueOf(0.5)).build();

        List<AnalysisStock> list = new ArrayList<>(List.of(b, a, aHigh));
        Collections.sort(list);
        // Within category "A", larger nextYearPEG comes first (DESC)
        assertEquals(aHigh, list.get(0));
        assertEquals(a, list.get(1));
        assertEquals(b, list.get(2));
    }
}

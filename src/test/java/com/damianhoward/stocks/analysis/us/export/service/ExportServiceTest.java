package com.damianhoward.stocks.analysis.us.export.service;

import com.damianhoward.stocks.analysis.us.analysis.domain.AnalysisStock;
import com.damianhoward.stocks.analysis.us.analysis.repository.AnalysisRepository;
import com.damianhoward.stocks.analysis.us.export.event.ExportCompleteEvent;
import com.damianhoward.stocks.analysis.us.export.event.ExportStartEvent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.context.ApplicationEventPublisher;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ExportServiceTest {

    private AnalysisRepository analysisRepository;
    private ExcelExport excelExport;
    private EmailExport emailExport;
    private ApplicationEventPublisher eventPublisher;
    private ExportService service;

    @BeforeEach
    void setUp() {
        analysisRepository = mock(AnalysisRepository.class);
        excelExport = mock(ExcelExport.class);
        emailExport = mock(EmailExport.class);
        eventPublisher = mock(ApplicationEventPublisher.class);
        service = new ExportService(analysisRepository, excelExport, emailExport, eventPublisher);
    }

    @Test
    void writesExcelAndEmailsWhenAnalysisStocksExist() {
        LocalDate date = LocalDate.of(2024, 5, 1);
        AnalysisStock stock = AnalysisStock.builder()
                .category("A").nextYearPEG(BigDecimal.ONE).build();
        Set<AnalysisStock> stocks = new LinkedHashSet<>();
        stocks.add(stock);
        when(analysisRepository.findByDate(date)).thenReturn(stocks);

        service.onAnalysisServiceEvent(new ExportStartEvent(date));

        verify(excelExport).generateExcel(anyList(), eq("./2024-05-01-stock-analysis.xls"));
        verify(emailExport).emailExport(
                eq(date),
                eq("2024-05-01-stock-analysis"),
                eq("2024-05-01-stock-analysis.xls"),
                eq("./2024-05-01-stock-analysis.xls"));
        verify(eventPublisher).publishEvent(any(ExportCompleteEvent.class));
    }

    @Test
    void emptyAnalysisStillPublishesCompletionButSkipsExports() {
        LocalDate date = LocalDate.of(2024, 5, 1);
        when(analysisRepository.findByDate(date)).thenReturn(Collections.emptySet());

        service.onAnalysisServiceEvent(new ExportStartEvent(date));

        verify(excelExport, never()).generateExcel(anyList(), anyString());
        verify(emailExport, never()).emailExport(any(), anyString(), anyString(), anyString());
        verify(eventPublisher).publishEvent(any(ExportCompleteEvent.class));
    }
}

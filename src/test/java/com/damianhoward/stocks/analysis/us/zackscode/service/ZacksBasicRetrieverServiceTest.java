package com.damianhoward.stocks.analysis.us.zackscode.service;

import com.damianhoward.stocks.analysis.us.zacksindustry.domain.ZacksList;
import com.damianhoward.stocks.analysis.us.zacksindustry.repository.ZacksListRepository;
import com.damianhoward.stocks.analysis.us.zackscode.domain.ZacksCode;
import com.damianhoward.stocks.analysis.us.zackscode.event.ZacksBasicCompleteEvent;
import com.damianhoward.stocks.analysis.us.zackscode.event.ZacksBasicStartEvent;
import com.damianhoward.stocks.analysis.us.zackscode.repository.ZacksBasicRepository;
import com.damianhoward.stocks.exception.DataRetrievalError;
import com.damianhoward.stocks.html.HtmlParser;
import com.damianhoward.stocks.html.HtmlResponse;
import com.damianhoward.stocks.html.HtmlRetriever;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.io.IOException;
import java.time.LocalDate;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ZacksBasicRetrieverServiceTest {

    private HtmlRetriever htmlRetriever;
    private HtmlParser htmlParser;
    private ZacksListRepository zacksListRepository;
    private ZacksBasicRepository zacksBasicRepository;
    private ApplicationEventPublisher eventPublisher;
    private ZacksBasicRetrieverService service;

    @BeforeEach
    void setUp() {
        htmlRetriever = mock(HtmlRetriever.class);
        htmlParser = mock(HtmlParser.class);
        zacksListRepository = mock(ZacksListRepository.class);
        zacksBasicRepository = mock(ZacksBasicRepository.class);
        eventPublisher = mock(ApplicationEventPublisher.class);
        TransactionTemplate transactionTemplate = new TransactionTemplate(mock(PlatformTransactionManager.class));
        service = new ZacksBasicRetrieverService(
                htmlRetriever, htmlParser, zacksListRepository, zacksBasicRepository, eventPublisher, transactionTemplate);
    }

    @Test
    void extractsCompaniesPerIndustryAndPersists() throws DataRetrievalError {
        LocalDate date = LocalDate.of(2024, 6, 1);
        ZacksList industry = new ZacksList();
        industry.setIndex("12");
        industry.setIndustry("Software");
        when(zacksListRepository.findByDate(date)).thenReturn(Set.of(industry));

        HtmlResponse response = new HtmlResponse();
        response.parsedHtml = "industry-html";
        when(htmlRetriever.getHtml(anyString())).thenReturn(response);

        // The service runs replace("\\", "") on the extracted row, so we keep
        // the fixture free of backslashes for clarity.
        String row =
                " \"Company\" : \"acme corp\" something rel=\"ACME\"," +
                " \"Company\" : \"globex inc\" something rel=\"GBLX\"";
        when(htmlParser.extractRow(anyString(), anyString())).thenReturn(row);

        service.onZacksBasicStartEvent(new ZacksBasicStartEvent(date));

        ArgumentCaptor<List<ZacksCode>> captor = ArgumentCaptor.forClass(List.class);
        verify(zacksBasicRepository).saveAll(captor.capture());
        List<ZacksCode> persisted = captor.getValue();
        assertEquals(2, persisted.size());
        assertEquals("Acme Corp", persisted.get(0).getCompany());  // WordUtils.capitalizeFully
        assertEquals("ACME", persisted.get(0).getZacksCode());
        assertEquals("Software", persisted.get(0).getIndustry());
        assertEquals(date, persisted.get(0).getDate());
        assertEquals("Globex Inc", persisted.get(1).getCompany());

        verify(zacksBasicRepository).deleteByDate(date);
        verify(eventPublisher).publishEvent(any(ZacksBasicCompleteEvent.class));
    }

    @Test
    void retrieverFailureForOneIndustryWrapsAsIllegalState() throws DataRetrievalError {
        LocalDate date = LocalDate.of(2024, 6, 1);
        ZacksList industry = new ZacksList();
        industry.setIndex("12");
        industry.setIndustry("Software");
        when(zacksListRepository.findByDate(date)).thenReturn(Set.of(industry));
        when(htmlRetriever.getHtml(anyString())).thenThrow(new DataRetrievalError(new IOException("boom")));

        assertThrows(IllegalStateException.class,
                () -> service.onZacksBasicStartEvent(new ZacksBasicStartEvent(date)));
        // One industry's fetch failed, so existing good data must NOT have been deleted.
        verify(zacksBasicRepository, never()).deleteByDate(any());
    }

    @Test
    void noIndustriesPublishesCompletionAnyway() {
        LocalDate date = LocalDate.of(2024, 6, 1);
        when(zacksListRepository.findByDate(date)).thenReturn(Set.of());

        service.onZacksBasicStartEvent(new ZacksBasicStartEvent(date));

        verify(zacksBasicRepository).deleteByDate(date);
        verify(eventPublisher).publishEvent(any(ZacksBasicCompleteEvent.class));
    }
}

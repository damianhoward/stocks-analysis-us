package com.damianhoward.stocks.analysis.us.zackscode.service;

import com.damianhoward.stocks.analysis.us.zacksindustry.domain.ZacksList;
import com.damianhoward.stocks.analysis.us.zacksindustry.repository.ZacksListRepository;
import com.damianhoward.stocks.analysis.us.zackscode.domain.ZacksCode;
import com.damianhoward.stocks.analysis.us.zackscode.event.ZacksBasicCompleteEvent;
import com.damianhoward.stocks.analysis.us.zackscode.event.ZacksBasicStartEvent;
import com.damianhoward.stocks.analysis.us.zackscode.repository.ZacksBasicRepository;
import com.damianhoward.stocks.exception.DataRetrievalError;
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
    private ZacksListRepository zacksListRepository;
    private ZacksBasicRepository zacksBasicRepository;
    private ApplicationEventPublisher eventPublisher;
    private ZacksBasicRetrieverService service;

    /**
     * The shape Zacks serves: a JavaScript object literal (columns is unquoted) whose symbol cell is
     * an HTML fragment with backslash-escaped quotes and slashes, and whose data marker is spaced.
     */
    private static final String INDUSTRY_PAGE = """
            Software^window.app_data =
                {
                columns : [ { "mDataProp"   : "Company" } , { "mDataProp"   : "Symbol" } ],
                "data"  : [  { "Company"    :  "acme corp", "Symbol"     :  "<button class=\\"modal_external\\" rel=\\"ACME\\">ACME Quick Quote<\\/button>" } ,  { "Company"    :  "globex inc", "Symbol"     :  "<button class=\\"modal_external\\" rel=\\"GBLX\\">GBLX Quick Quote<\\/button>" }  ]
                }
            """;

    @BeforeEach
    void setUp() {
        htmlRetriever = mock(HtmlRetriever.class);
        zacksListRepository = mock(ZacksListRepository.class);
        zacksBasicRepository = mock(ZacksBasicRepository.class);
        eventPublisher = mock(ApplicationEventPublisher.class);
        TransactionTemplate transactionTemplate = new TransactionTemplate(mock(PlatformTransactionManager.class));
        service = new ZacksBasicRetrieverService(
                htmlRetriever, zacksListRepository, zacksBasicRepository, eventPublisher, transactionTemplate);
    }

    @Test
    void extractsCompaniesPerIndustryAndPersists() throws DataRetrievalError {
        LocalDate date = LocalDate.of(2024, 6, 1);
        ZacksList industry = ZacksList.of(date, "12", null, "Software");
        when(zacksListRepository.findByDate(date)).thenReturn(Set.of(industry));

        when(htmlRetriever.getHtml(anyString())).thenReturn(INDUSTRY_PAGE);

        service.onZacksBasicStartEvent(new ZacksBasicStartEvent(date));

        ArgumentCaptor<List<ZacksCode>> captor = ArgumentCaptor.forClass(List.class);
        verify(zacksBasicRepository).saveAll(captor.capture());
        List<ZacksCode> persisted = captor.getValue();
        assertEquals(2, persisted.size());
        assertEquals("Acme Corp", persisted.get(0).company());  // WordUtils.capitalizeFully
        assertEquals("ACME", persisted.get(0).zacksCode());
        assertEquals("Software", persisted.get(0).industry());
        assertEquals(date, persisted.get(0).date());
        assertEquals("Globex Inc", persisted.get(1).company());

        verify(zacksBasicRepository).deleteByDate(date);
        verify(eventPublisher).publishEvent(any(ZacksBasicCompleteEvent.class));
    }

    @Test
    void aPageWithoutTheDataArrayFailsRatherThanDroppingTheIndustry() throws DataRetrievalError {
        LocalDate date = LocalDate.of(2024, 6, 1);
        when(zacksListRepository.findByDate(date)).thenReturn(Set.of(ZacksList.of(date, "12", null, "Software")));
        when(htmlRetriever.getHtml(anyString())).thenReturn("<html>Zacks is down for maintenance</html>");

        assertThrows(IllegalStateException.class,
                () -> service.onZacksBasicStartEvent(new ZacksBasicStartEvent(date)));
        verify(zacksBasicRepository, never()).deleteByDate(any());
    }

    @Test
    void retrieverFailureForOneIndustryWrapsAsIllegalState() throws DataRetrievalError {
        LocalDate date = LocalDate.of(2024, 6, 1);
        ZacksList industry = ZacksList.of(date, "12", null, "Software");
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

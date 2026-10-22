package com.damianhoward.stocks.analysis.us.sectormapping.service;

import com.damianhoward.stocks.analysis.us.sectormapping.domain.ZacksSectorMapping;
import com.damianhoward.stocks.analysis.us.sectormapping.event.ZacksSectorMappingCompleteEvent;
import com.damianhoward.stocks.analysis.us.sectormapping.event.ZacksSectorMappingStartEvent;
import com.damianhoward.stocks.analysis.us.sectormapping.repository.ZacksSectorMappingRepository;
import com.damianhoward.stocks.exception.DataRetrievalError;
import com.damianhoward.stocks.html.HtmlRetriever;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ZacksSectorMappingServiceTest {

    private HtmlRetriever htmlRetriever;
    private ZacksSectorMappingRepository repository;
    private ApplicationEventPublisher eventPublisher;
    private ZacksSectorMappingService service;

    @BeforeEach
    void setUp() {
        htmlRetriever = mock(HtmlRetriever.class);
        repository = mock(ZacksSectorMappingRepository.class);
        eventPublisher = mock(ApplicationEventPublisher.class);
        TransactionTemplate transactionTemplate = new TransactionTemplate(mock(PlatformTransactionManager.class));
        service = new ZacksSectorMappingService(htmlRetriever, repository, eventPublisher, transactionTemplate);
    }

    @Test
    void parsesZacksAppDataAndPersistsAfterDroppingHeaderRow() throws DataRetrievalError {
        // The service finds "window.app_data =" and reads the literal after it. The first row is
        // Zacks's Index pseudo-sector and is dropped, so we ship 3 rows and expect 2.
        String json =
                "{\"data\":[" +
                "{\"Sector Group\":\"<span title=\\\"HDR_S\\\">.</span>\"," +
                " \"Medium(M) Industry Group\":\"<span title=\\\"HDR_M\\\">.</span>\"," +
                " \"Expanded(X) Industry Group\":\"<span title=\\\"HDR_X\\\">.</span>\"}," +
                "{\"Sector Group\":\"<span title=\\\"Tech\\\">x</span>\"," +
                " \"Medium(M) Industry Group\":\"<span title=\\\"Apps\\\">x</span>\"," +
                " \"Expanded(X) Industry Group\":\"<span title=\\\"SaaS\\\">x</span>\"," +
                " \"Unused\":\"ignore-me\"}," +
                "{\"Sector Group\":\"<span title=\\\"Finance\\\">y</span>\"," +
                " \"Medium(M) Industry Group\":\"<span title=\\\"Banks\\\">y</span>\"," +
                " \"Expanded(X) Industry Group\":\"<span title=\\\"Retail Banks\\\">y</span>\"}" +
                "],\"other\":\"ignored\"}";
        String body = "junk-prefix window.app_data = " + json;
        when(htmlRetriever.getHtml(anyString())).thenReturn(body);

        LocalDate date = LocalDate.of(2024, 6, 1);
        service.onZacksSectorMappingStartEvent(new ZacksSectorMappingStartEvent(date));

        verify(repository).deleteByDate(date);
        ArgumentCaptor<List<ZacksSectorMapping>> captor = ArgumentCaptor.forClass(List.class);
        verify(repository).saveAll(captor.capture());
        List<ZacksSectorMapping> saved = captor.getValue();
        assertEquals(2, saved.size(), "the first row is dropped from saved entries");

        ZacksSectorMapping first = saved.get(0);
        assertEquals("Tech", first.sectorGroup());
        assertEquals("Apps", first.mediumIndustryGroup());
        assertEquals("SaaS", first.industry());
        assertEquals(date, first.date());

        ZacksSectorMapping second = saved.get(1);
        assertEquals("Finance", second.sectorGroup());
        assertEquals("Retail Banks", second.industry());

        verify(eventPublisher).publishEvent(any(ZacksSectorMappingCompleteEvent.class));
    }

    @Test
    void readsTheJavaScriptLiteralZacksServesAndStopsAtItsEnd() throws DataRetrievalError {
        // The page's app_data is a JavaScript object literal, not JSON: columns is unquoted, and
        // the closing script tag and the rest of the page follow it.
        String page = """
                <html><script>window.app_data =
                                {

                \t\t\t\t    columns : [
                                    { "mDataProp"   : "Sector Group"
                                    , "sTitle"      : "Sector Group"
                                    }
                                ],
                         "data"  : [  { "Sector Group"               :  "<span title=\\"Index\\" >Index</span>", "Medium(M) Industry Group"   :  "<span title=\\"Indices\\" >Indices</span>", "Expanded(X) Industry Group" :  "<span title=\\"Indicies\\" >Indicies</span>" } ,  { "Sector Group"               :  "<span title=\\"Consumer Staples\\" >Consumer Staple...</span>", "Medium(M) Industry Group"   :  "<span title=\\"Publishing\\" >Publishing</span>", "Expanded(X) Industry Group" :  "<span title=\\"Publishing - Newspapers\\" >Publishing - Ne...</span>" }  ]
                                }</script>        <script defer src="https://staticx.zacks.com/js/x.js"></script>
                <section><h1>Sector and Industry Group Classification</h1></section></html>
                """;
        when(htmlRetriever.getHtml(anyString())).thenReturn(page);

        LocalDate date = LocalDate.of(2024, 6, 1);
        service.onZacksSectorMappingStartEvent(new ZacksSectorMappingStartEvent(date));

        ArgumentCaptor<List<ZacksSectorMapping>> captor = ArgumentCaptor.forClass(List.class);
        verify(repository).saveAll(captor.capture());
        List<ZacksSectorMapping> saved = captor.getValue();
        assertEquals(1, saved.size(), "the Index pseudo-sector is dropped");
        // The span title carries the full name where the visible text is truncated.
        assertEquals("Consumer Staples", saved.get(0).sectorGroup());
        assertEquals("Publishing - Newspapers", saved.get(0).industry());
    }

    @Test
    void aPageWithoutAppDataFailsInsteadOfWipingTheDate() throws DataRetrievalError {
        when(htmlRetriever.getHtml(anyString())).thenReturn("<html>Zacks is down for maintenance</html>");

        assertThrows(IllegalStateException.class, () -> service.onZacksSectorMappingStartEvent(
                new ZacksSectorMappingStartEvent(LocalDate.of(2024, 6, 1))));
        verify(repository, never()).deleteByDate(any());
        verify(eventPublisher, never()).publishEvent(any());
    }

    @Test
    void appDataWithNoRowsFailsInsteadOfWipingTheDate() throws DataRetrievalError {
        when(htmlRetriever.getHtml(anyString())).thenReturn("window.app_data = { columns : [], \"data\" : [] }");

        assertThrows(IllegalStateException.class, () -> service.onZacksSectorMappingStartEvent(
                new ZacksSectorMappingStartEvent(LocalDate.of(2024, 6, 1))));
        verify(repository, never()).deleteByDate(any());
    }

    @Test
    void retrieverFailureWrapsAsIllegalState() throws DataRetrievalError {
        when(htmlRetriever.getHtml(anyString())).thenThrow(new DataRetrievalError(new java.io.IOException("boom")));

        IllegalStateException ex = assertThrows(IllegalStateException.class, () ->
                service.onZacksSectorMappingStartEvent(
                        new ZacksSectorMappingStartEvent(LocalDate.of(2024, 6, 1))));
        org.junit.jupiter.api.Assertions.assertTrue(ex.getMessage().contains("Unable to download Zacks sector mapping"));
        // The download failed, so existing good data must NOT have been deleted.
        verify(repository, never()).deleteByDate(any());
    }
}

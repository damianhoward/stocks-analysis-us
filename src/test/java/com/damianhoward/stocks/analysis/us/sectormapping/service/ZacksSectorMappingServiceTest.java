package com.damianhoward.stocks.analysis.us.sectormapping.service;

import com.damianhoward.stocks.analysis.us.sectormapping.domain.ZacksSectorMapping;
import com.damianhoward.stocks.analysis.us.sectormapping.event.ZacksSectorMappingCompleteEvent;
import com.damianhoward.stocks.analysis.us.sectormapping.event.ZacksSectorMappingStartEvent;
import com.damianhoward.stocks.analysis.us.sectormapping.repository.ZacksSectorMappingRepository;
import com.damianhoward.stocks.exception.DataRetrievalError;
import com.damianhoward.stocks.html.HtmlResponse;
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
        // The service finds "window.app_data =", then JSON-parses everything
        // after it. It also drops the first parsed row as a header; so we ship
        // 3 rows and the first one is dropped.
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
        HtmlResponse response = new HtmlResponse();
        response.rawHtml = body;
        when(htmlRetriever.getHtml(anyString())).thenReturn(response);

        LocalDate date = LocalDate.of(2024, 6, 1);
        service.onZacksSectorMappingStartEvent(new ZacksSectorMappingStartEvent(date));

        verify(repository).deleteByDate(date);
        ArgumentCaptor<List<ZacksSectorMapping>> captor = ArgumentCaptor.forClass(List.class);
        verify(repository).saveAll(captor.capture());
        List<ZacksSectorMapping> saved = captor.getValue();
        assertEquals(2, saved.size(), "header row is dropped from saved entries");

        ZacksSectorMapping first = saved.get(0);
        assertEquals("Tech", first.getSectorGroup());
        assertEquals("Apps", first.getMediumIndustryGroup());
        assertEquals("SaaS", first.getIndustry());
        assertEquals(date, first.getDate());

        ZacksSectorMapping second = saved.get(1);
        assertEquals("Finance", second.getSectorGroup());
        assertEquals("Retail Banks", second.getIndustry());

        verify(eventPublisher).publishEvent(any(ZacksSectorMappingCompleteEvent.class));
    }

    @Test
    void tinyHtmlBelowMarkerLengthShortCircuitsAndStillPublishesEvent() throws DataRetrievalError {
        // The body must be shorter than startWord.length() (16 chars) for the
        // early-return path; otherwise the service tries to parse junk as JSON
        // and throws.
        HtmlResponse response = new HtmlResponse();
        response.rawHtml = "tiny";
        when(htmlRetriever.getHtml(anyString())).thenReturn(response);

        LocalDate date = LocalDate.of(2024, 6, 1);
        service.onZacksSectorMappingStartEvent(new ZacksSectorMappingStartEvent(date));

        verify(repository).deleteByDate(date);
        verify(eventPublisher).publishEvent(any(ZacksSectorMappingCompleteEvent.class));
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

package com.damianhoward.stocks.analysis.us.zacksindustry.service;

import com.damianhoward.stocks.analysis.us.zacksindustry.domain.ZacksList;
import com.damianhoward.stocks.analysis.us.zacksindustry.event.ZacksListCompleteEvent;
import com.damianhoward.stocks.analysis.us.zacksindustry.event.ZacksListStartEvent;
import com.damianhoward.stocks.analysis.us.zacksindustry.repository.ZacksListRepository;
import com.damianhoward.stocks.exception.DataRetrievalError;
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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ZacksListRetrieverServiceTest {

    private HtmlRetriever htmlRetriever;
    private ZacksListRepository repository;
    private ApplicationEventPublisher eventPublisher;
    private ZacksListRetrieverService service;

    @BeforeEach
    void setUp() {
        htmlRetriever = mock(HtmlRetriever.class);
        repository = mock(ZacksListRepository.class);
        eventPublisher = mock(ApplicationEventPublisher.class);
        TransactionTemplate transactionTemplate = new TransactionTemplate(mock(PlatformTransactionManager.class));
        service = new ZacksListRetrieverService(htmlRetriever, repository, eventPublisher, transactionTemplate);
    }

    @Test
    void parsesJsonDataArrayAndSavesEntries() throws DataRetrievalError {
        String json = "{\"data\":[" +
                "{\"industry_id\":\"5\"," +
                "  \"industry_name\":\"<a href='zacks.com/x'>Banking</a>\"," +
                "  \"no_of_stocks\":\"42\"," +
                "  \"junk_array\":[1,2,3]," +
                "  \"junk_object\":{\"k\":\"v\"}," +
                "  \"ignored_scalar\":\"hello\"}," +
                "{\"industry_id\":\"2\"," +
                "  \"industry_name\":\"<a href='zacks.com/y'>Tech</a>\"," +
                "  \"no_of_stocks\":\"7\"}" +
                "],\"meta\":{\"k\":\"v\"}}";
        HtmlResponse response = new HtmlResponse();
        response.parsedHtml = json;
        when(htmlRetriever.getHtml(anyString())).thenReturn(response);

        LocalDate date = LocalDate.of(2024, 6, 1);
        service.onZacksListStartEvent(new ZacksListStartEvent(date));

        ArgumentCaptor<List<ZacksList>> captor = ArgumentCaptor.forClass(List.class);
        verify(repository).saveAll(captor.capture());
        List<ZacksList> saved = captor.getValue();
        // Sorted ascending by industry index
        assertEquals(2, saved.size());
        assertEquals("2", saved.get(0).getIndex());
        assertEquals("Tech", saved.get(0).getIndustry());
        assertEquals("5", saved.get(1).getIndex());
        assertEquals("Banking", saved.get(1).getIndustry());
        assertEquals("42", saved.get(1).getTotal());
        assertEquals(date, saved.get(1).getDate());

        verify(repository).deleteByDate(date);
        verify(eventPublisher).publishEvent(any(ZacksListCompleteEvent.class));
    }

    @Test
    void retrieverFailureSurfacesAsIllegalState() throws DataRetrievalError {
        when(htmlRetriever.getHtml(anyString())).thenThrow(new DataRetrievalError(new IOException("net")));
        assertThrows(IllegalStateException.class,
                () -> service.onZacksListStartEvent(new ZacksListStartEvent(LocalDate.now())));
        // The fetch failed, so existing good data must NOT have been deleted.
        verify(repository, never()).deleteByDate(any());
    }
}

package com.damianhoward.stocks.analysis.us.stocklookup.service;

import com.damianhoward.stocks.analysis.us.zackscode.domain.ZacksCode;
import com.damianhoward.stocks.analysis.us.zackscode.repository.ZacksBasicRepository;
import com.damianhoward.stocks.analysis.us.stocklookup.domain.StockLookup;
import com.damianhoward.stocks.analysis.us.stocklookup.event.StockLookupCompleteEvent;
import com.damianhoward.stocks.analysis.us.stocklookup.event.StockLookupStartEvent;
import com.damianhoward.stocks.analysis.us.stocklookup.repository.StockLookupRepository;
import com.damianhoward.stocks.analysis.us.stocklookup.service.yahoo.YahooStockLookup;
import com.damianhoward.stocks.exception.DataRetrievalError;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDate;
import java.util.LinkedHashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class StockLookupServiceTest {

    private ZacksBasicRepository zacksBasicRepository;
    private StockLookupRepository stockLookupRepository;
    private YahooStockLookup yahooStockLookup;
    private ApplicationEventPublisher eventPublisher;
    private StockLookupService service;

    @BeforeEach
    void setUp() {
        zacksBasicRepository = mock(ZacksBasicRepository.class);
        stockLookupRepository = mock(StockLookupRepository.class);
        yahooStockLookup = mock(YahooStockLookup.class);
        eventPublisher = mock(ApplicationEventPublisher.class);

        service = new StockLookupService(
                zacksBasicRepository, stockLookupRepository, yahooStockLookup, eventPublisher);
        // Force throttle to a no-op; default min/max values from @Value won't be
        // wired up without a real ApplicationContext.
        ReflectionTestUtils.setField(service, "sleepTimeMin", 0);
        ReflectionTestUtils.setField(service, "sleepTimeMax", 0);
        ReflectionTestUtils.setField(service, "lookupConcurrency", 4);
    }

    @AfterEach
    void clearSystemProperty() {
        System.clearProperty("zacksDate");
    }

    @Test
    void looksUpEachNonExistingZacksCodeAndSaves() throws DataRetrievalError {
        LocalDate date = LocalDate.of(2024, 6, 1);

        ZacksCode code = newZacks("ACME");
        ZacksCode existingCode = newZacks("EXST");
        when(zacksBasicRepository.findByDate(date)).thenReturn(new LinkedHashSet<>(Set.of(code, existingCode)));

        StockLookup existing = StockLookup.builder().zacksCode("EXST").date(date).build();
        when(stockLookupRepository.findByDate(date)).thenReturn(Set.of(existing));

        StockLookup yahooResult = StockLookup.builder().company("Acme Inc").build();
        when(yahooStockLookup.lookup("ACME")).thenReturn(yahooResult);

        service.onStockLookupStartEvent(new StockLookupStartEvent(date));

        ArgumentCaptor<StockLookup> captor = ArgumentCaptor.forClass(StockLookup.class);
        verify(stockLookupRepository, times(1)).save(captor.capture());
        StockLookup saved = captor.getValue();
        assertEquals("ACME", saved.getZacksCode(), "ACME isn't in existing lookups so it's the only call");
        assertEquals(date, saved.getDate(), "service must stamp the event date");
        assertNotNull(saved.getId(), "service must assign a fresh id");

        verify(eventPublisher).publishEvent(any(StockLookupCompleteEvent.class));
    }

    @Test
    void yahooFailureRecordsAnErrorLookupAndContinues() throws DataRetrievalError {
        LocalDate date = LocalDate.of(2024, 6, 1);
        when(zacksBasicRepository.findByDate(date)).thenReturn(Set.of(newZacks("OOPS")));
        when(stockLookupRepository.findByDate(date)).thenReturn(Set.of());

        when(yahooStockLookup.lookup("OOPS")).thenThrow(new DataRetrievalError(new java.io.IOException("yahoo down")));

        service.onStockLookupStartEvent(new StockLookupStartEvent(date));

        ArgumentCaptor<StockLookup> captor = ArgumentCaptor.forClass(StockLookup.class);
        verify(stockLookupRepository).save(captor.capture());
        StockLookup error = captor.getValue();
        assertEquals("OOPS", error.getZacksCode());
        org.junit.jupiter.api.Assertions.assertNotNull(error.getErrorMessage());

        verify(eventPublisher).publishEvent(any(StockLookupCompleteEvent.class));
    }

    @Test
    void truncatesLongErrorMessagesToTwoHundredChars() throws DataRetrievalError {
        LocalDate date = LocalDate.of(2024, 6, 1);
        when(zacksBasicRepository.findByDate(date)).thenReturn(Set.of(newZacks("BIG")));
        when(stockLookupRepository.findByDate(date)).thenReturn(Set.of());

        // A 400-char message becomes <=200
        String giant = "x".repeat(400);
        when(yahooStockLookup.lookup("BIG")).thenThrow(new RuntimeException(giant));

        service.onStockLookupStartEvent(new StockLookupStartEvent(date));

        ArgumentCaptor<StockLookup> captor = ArgumentCaptor.forClass(StockLookup.class);
        verify(stockLookupRepository).save(captor.capture());
        assertEquals(200, captor.getValue().getErrorMessage().length());
    }

    @Test
    void looksUpEveryCodeThroughTheConcurrentPool() throws DataRetrievalError {
        LocalDate date = LocalDate.of(2024, 6, 1);
        Set<ZacksCode> codes = new LinkedHashSet<>();
        for (int i = 0; i < 25; i++) {
            codes.add(newZacks("C" + i));
        }
        when(zacksBasicRepository.findByDate(date)).thenReturn(codes);
        when(stockLookupRepository.findByDate(date)).thenReturn(Set.of());
        // Fresh result per call — mirrors the real lookup and avoids shared mutation across threads.
        when(yahooStockLookup.lookup(anyString()))
                .thenAnswer(invocation -> StockLookup.builder().company("X").build());

        service.onStockLookupStartEvent(new StockLookupStartEvent(date));

        // Every code is looked up and saved exactly once despite running across the pool.
        verify(yahooStockLookup, times(25)).lookup(anyString());
        verify(stockLookupRepository, times(25)).save(any(StockLookup.class));
        verify(eventPublisher).publishEvent(any(StockLookupCompleteEvent.class));
    }

    @Test
    void aWorkerFailingOutsideItsOwnCatchFailsTheBatch() throws DataRetrievalError {
        LocalDate date = LocalDate.of(2024, 6, 1);
        when(zacksBasicRepository.findByDate(date)).thenReturn(new LinkedHashSet<>(Set.of(newZacks("BOOM"))));
        when(stockLookupRepository.findByDate(date)).thenReturn(Set.of());

        // The lookup fails, so the service falls into its catch and tries to write an error row —
        // and that write fails too. Nothing inside lookupAndSave catches this, so it reaches the
        // Future, which is exactly the failure invokeAll used to discard.
        when(yahooStockLookup.lookup("BOOM")).thenThrow(new DataRetrievalError(new java.io.IOException("yahoo down")));
        when(stockLookupRepository.save(any(StockLookup.class))).thenThrow(new RuntimeException("database down"));

        IllegalStateException thrown = org.junit.jupiter.api.Assertions.assertThrows(IllegalStateException.class,
                () -> service.onStockLookupStartEvent(new StockLookupStartEvent(date)));

        assertEquals("1 of 1 stock lookups failed; the batch is incomplete", thrown.getMessage());
        assertNotNull(thrown.getCause(), "the first worker failure must be preserved as the cause");
        assertEquals("database down", thrown.getCause().getMessage());

        // The batch never announces itself as complete when it lost a code.
        verify(eventPublisher, org.mockito.Mockito.never()).publishEvent(any(StockLookupCompleteEvent.class));
    }

    @Test
    void oneFailedWorkerDoesNotStopTheOthersFromRunning() throws DataRetrievalError {
        LocalDate date = LocalDate.of(2024, 6, 1);
        Set<ZacksCode> codes = new LinkedHashSet<>();
        for (int i = 0; i < 10; i++) {
            codes.add(newZacks("C" + i));
        }
        when(zacksBasicRepository.findByDate(date)).thenReturn(codes);
        when(stockLookupRepository.findByDate(date)).thenReturn(Set.of());
        when(yahooStockLookup.lookup(anyString()))
                .thenAnswer(invocation -> StockLookup.builder().company("X").build());
        // Only this one code's save blows up; the pool still runs every task to completion.
        when(stockLookupRepository.save(any(StockLookup.class))).thenAnswer(invocation -> {
            StockLookup saved = invocation.getArgument(0);
            if ("C7".equals(saved.getZacksCode())) {
                throw new RuntimeException("row rejected");
            }
            return saved;
        });

        IllegalStateException thrown = org.junit.jupiter.api.Assertions.assertThrows(IllegalStateException.class,
                () -> service.onStockLookupStartEvent(new StockLookupStartEvent(date)));

        assertEquals("1 of 10 stock lookups failed; the batch is incomplete", thrown.getMessage());
        verify(yahooStockLookup, times(10)).lookup(anyString());
        // 11 saves, not 10: C7's row is rejected, lookupAndSave catches that and tries to write an
        // error row for the same code, and that second write is rejected too — which is what escapes
        // into the Future.
        verify(stockLookupRepository, times(11)).save(any(StockLookup.class));
        verify(eventPublisher, org.mockito.Mockito.never()).publishEvent(any(StockLookupCompleteEvent.class));
    }

    @Test
    void nullErrorMessageIsStoredAsNull() throws DataRetrievalError {
        LocalDate date = LocalDate.of(2024, 6, 1);
        when(zacksBasicRepository.findByDate(date)).thenReturn(Set.of(newZacks("NULLMSG")));
        when(stockLookupRepository.findByDate(date)).thenReturn(Set.of());

        // An exception with no message exercises the null branch of truncate().
        when(yahooStockLookup.lookup("NULLMSG")).thenThrow(new RuntimeException());

        service.onStockLookupStartEvent(new StockLookupStartEvent(date));

        ArgumentCaptor<StockLookup> captor = ArgumentCaptor.forClass(StockLookup.class);
        verify(stockLookupRepository).save(captor.capture());
        org.junit.jupiter.api.Assertions.assertNull(captor.getValue().getErrorMessage());
    }

    @Test
    void throttleInterruptedSurfacesAsIllegalState() {
        // Drive the real throttle path: a positive window means it sleeps, and a
        // pre-set interrupt makes Thread.sleep abort immediately.
        ReflectionTestUtils.setField(service, "sleepTimeMin", 1);
        ReflectionTestUtils.setField(service, "sleepTimeMax", 1);

        Thread.currentThread().interrupt();
        try {
            org.junit.jupiter.api.Assertions.assertThrows(IllegalStateException.class,
                    () -> ReflectionTestUtils.invokeMethod(service, "sleepBetweenLookups"));
        } finally {
            // Clear the flag so it can't leak into other tests.
            Thread.interrupted();
        }
    }

    @Test
    void zacksDateSystemPropertyOverridesEventDate() {
        LocalDate eventDate = LocalDate.of(2024, 6, 1);
        LocalDate zacksDate = LocalDate.of(2024, 5, 1);
        System.setProperty("zacksDate", zacksDate.toString());
        when(zacksBasicRepository.findByDate(zacksDate)).thenReturn(Set.of());
        when(stockLookupRepository.findByDate(eventDate)).thenReturn(Set.of());

        service.onStockLookupStartEvent(new StockLookupStartEvent(eventDate));

        verify(zacksBasicRepository).findByDate(zacksDate);
        verify(stockLookupRepository).findByDate(eventDate);
    }

    private static ZacksCode newZacks(String code) {
        ZacksCode z = new ZacksCode();
        z.setZacksCode(code);
        z.setIndustry("any");
        return z;
    }
}

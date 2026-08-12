package io.github.damian1000.stocks;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.damian1000.stocks.analysis.us.analysis.event.AnalysisStockStartEvent;
import io.github.damian1000.stocks.analysis.us.export.event.ExportStartEvent;
import io.github.damian1000.stocks.analysis.us.sectormapping.event.ZacksSectorMappingStartEvent;
import io.github.damian1000.stocks.analysis.us.stocklookup.event.StockLookupStartEvent;
import io.github.damian1000.stocks.analysis.us.zackscode.event.ZacksBasicStartEvent;
import io.github.damian1000.stocks.analysis.us.zacksindustry.event.ZacksListStartEvent;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;

class StartEventResolverTest {

    private final LocalDate date = LocalDate.of(2026, 7, 5);

    @Test
    void missingNameDefaultsToThePipelinesFirstStage() {
        assertInstanceOf(ZacksSectorMappingStartEvent.class, StartEventResolver.resolve(null, date));
        assertInstanceOf(ZacksSectorMappingStartEvent.class, StartEventResolver.resolve("", date));
    }

    @Test
    void resolvesEveryStageByItsEventName() {
        assertInstanceOf(
                ZacksSectorMappingStartEvent.class, StartEventResolver.resolve("ZacksSectorMappingStartEvent", date));
        assertInstanceOf(ZacksListStartEvent.class, StartEventResolver.resolve("ZacksListStartEvent", date));
        assertInstanceOf(ZacksBasicStartEvent.class, StartEventResolver.resolve("ZacksBasicStartEvent", date));
        assertInstanceOf(StockLookupStartEvent.class, StartEventResolver.resolve("StockLookupStartEvent", date));
        assertInstanceOf(AnalysisStockStartEvent.class, StartEventResolver.resolve("AnalysisStockStartEvent", date));
        assertInstanceOf(ExportStartEvent.class, StartEventResolver.resolve("ExportStartEvent", date));
    }

    @Test
    void unknownNameFailsFastListingTheAllowedValues() {
        IllegalArgumentException ex = assertThrows(
                IllegalArgumentException.class, () -> StartEventResolver.resolve("ExprotStartEvent", date));

        assertTrue(ex.getMessage().contains("ExprotStartEvent"), "names the bad input: " + ex.getMessage());
        assertTrue(ex.getMessage().contains("ZacksSectorMappingStartEvent"), "lists alternatives: " + ex.getMessage());
        assertTrue(ex.getMessage().contains("ExportStartEvent"), "lists alternatives: " + ex.getMessage());
    }

    @Test
    void resolvedEventCarriesTheRequestedDate() {
        assertEquals(date, StartEventResolver.resolve("ExportStartEvent", date).date());
    }
}

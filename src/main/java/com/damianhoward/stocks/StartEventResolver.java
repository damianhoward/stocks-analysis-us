package com.damianhoward.stocks;

import com.damianhoward.stocks.analysis.us.analysis.event.AnalysisStockStartEvent;
import com.damianhoward.stocks.analysis.us.export.event.ExportStartEvent;
import com.damianhoward.stocks.analysis.us.sectormapping.event.ZacksSectorMappingStartEvent;
import com.damianhoward.stocks.analysis.us.stocklookup.event.StockLookupStartEvent;
import com.damianhoward.stocks.analysis.us.zackscode.event.ZacksBasicStartEvent;
import com.damianhoward.stocks.analysis.us.zacksindustry.event.ZacksListStartEvent;
import com.damianhoward.stocks.event.Event;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.Map;
import org.apache.commons.lang3.StringUtils;

/**
 * Maps the {@code -Devent} system property to the pipeline start event it names. Resolved before
 * Spring boots, so a typo fails fast with the allowed names instead of surfacing as a null-event
 * error after a full context start.
 */
final class StartEventResolver {

    private StartEventResolver() {}

    /** Resolves {@code eventName} to its start event; null/empty means the pipeline's first stage. */
    static Event resolve(String eventName, LocalDate date) {
        Map<String, Event> events = new LinkedHashMap<>();
        events.put(ZacksSectorMappingStartEvent.class.getSimpleName(), new ZacksSectorMappingStartEvent(date));
        events.put(ZacksListStartEvent.class.getSimpleName(), new ZacksListStartEvent(date));
        events.put(ZacksBasicStartEvent.class.getSimpleName(), new ZacksBasicStartEvent(date));
        events.put(StockLookupStartEvent.class.getSimpleName(), new StockLookupStartEvent(date));
        events.put(AnalysisStockStartEvent.class.getSimpleName(), new AnalysisStockStartEvent(date));
        events.put(ExportStartEvent.class.getSimpleName(), new ExportStartEvent(date));

        if (StringUtils.isEmpty(eventName)) {
            return events.get(ZacksSectorMappingStartEvent.class.getSimpleName());
        }
        Event startEvent = events.get(eventName);
        if (startEvent == null) {
            throw new IllegalArgumentException(
                    "Unknown event '" + eventName + "'. Allowed values: " + String.join(", ", events.keySet()));
        }
        return startEvent;
    }
}

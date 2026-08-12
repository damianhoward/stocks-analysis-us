package com.damianhoward.stocks.analysis.us;

import com.damianhoward.stocks.analysis.us.analysis.event.AnalysisStockCompleteEvent;
import com.damianhoward.stocks.analysis.us.analysis.event.AnalysisStockStartEvent;
import com.damianhoward.stocks.analysis.us.export.event.ExportCompleteEvent;
import com.damianhoward.stocks.analysis.us.export.event.ExportStartEvent;
import com.damianhoward.stocks.analysis.us.sectormapping.event.ZacksSectorMappingCompleteEvent;
import com.damianhoward.stocks.analysis.us.stocklookup.event.StockLookupCompleteEvent;
import com.damianhoward.stocks.analysis.us.stocklookup.event.StockLookupStartEvent;
import com.damianhoward.stocks.analysis.us.zackscode.event.ZacksBasicCompleteEvent;
import com.damianhoward.stocks.analysis.us.zackscode.event.ZacksBasicStartEvent;
import com.damianhoward.stocks.analysis.us.zacksindustry.event.ZacksListCompleteEvent;
import com.damianhoward.stocks.analysis.us.zacksindustry.event.ZacksListStartEvent;
import com.damianhoward.stocks.event.Event;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Component
/**
 * The pipeline's order, expressed as event handlers: each stage's Complete event publishes the
 * next stage's Start event. Sector mapping to Zacks list to Zacks basic to stock lookup to
 * analysis to export.
 *
 * <p>This class is the only place that order exists. No stage knows what follows it, which is
 * what lets a stage be run alone; changing the sequence is a change here and nowhere else.
 */
public class PipelineSequencer {

    private static final Logger log = LoggerFactory.getLogger(PipelineSequencer.class);

    private ApplicationEventPublisher eventPublisher;

    public PipelineSequencer(ApplicationEventPublisher eventPublisher) {
        this.eventPublisher = eventPublisher;
    }

    @EventListener
    public void onZacksSectorMappingCompleteEvent(ZacksSectorMappingCompleteEvent event) {
        log.info("onZacksSectorMappingCompleteEvent {}", event);
        Event nextEvent = new ZacksListStartEvent(event.date());
        log.info("publishing next event {}", nextEvent);
        eventPublisher.publishEvent(nextEvent);
    }

    @EventListener
    public void onZacksListCompleteEvent(ZacksListCompleteEvent event) {
        log.info("onZacksListCompleteEvent {}", event);
        Event nextEvent = new ZacksBasicStartEvent(event.date());
        log.info("publishing next event {}", nextEvent);
        eventPublisher.publishEvent(nextEvent);
    }

    @EventListener
    public void onZacksBasicCompleteEvent(ZacksBasicCompleteEvent event) {
        log.info("onZacksBasicCompleteEvent {}", event);
        Event nextEvent = new StockLookupStartEvent(event.date());
        log.info("publishing next event {}", nextEvent);
        eventPublisher.publishEvent(nextEvent);
    }

    @EventListener
    public void onStockLookupCompleteEvent(StockLookupCompleteEvent event) {
        log.info("onStockLookupCompleteEvent {}", event);
        Event nextEvent = new AnalysisStockStartEvent(event.date());
        log.info("publishing next event {}", nextEvent);
        eventPublisher.publishEvent(nextEvent);
    }

    @EventListener
    public void onAnalysisStockCompleteEvent(AnalysisStockCompleteEvent event) {
        log.info("onAnalysisStockCompleteEvent {}", event);
        Event nextEvent = new ExportStartEvent(event.date());
        log.info("publishing next event {}", nextEvent);
        eventPublisher.publishEvent(nextEvent);
    }

    @EventListener
    public void onExportCompleteEvent(ExportCompleteEvent event) {
       log.info("onExportCompleteEvent {}", event);
    }

}

package com.damianhoward.stocks.analysis.us.export.event;

import com.damianhoward.stocks.event.Event;
import java.time.LocalDate;

/** Pipeline stage boundary for the run keyed on {@code date}. */
public record ExportStartEvent(LocalDate date) implements Event {}

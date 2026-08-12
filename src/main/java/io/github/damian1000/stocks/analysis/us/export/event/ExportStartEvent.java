package io.github.damian1000.stocks.analysis.us.export.event;

import io.github.damian1000.stocks.event.Event;
import java.time.LocalDate;

/** Pipeline stage boundary for the run keyed on {@code date}. */
public record ExportStartEvent(LocalDate date) implements Event {}

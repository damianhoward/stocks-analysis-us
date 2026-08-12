package io.github.damian1000.stocks.analysis.us.zackscode.event;

import io.github.damian1000.stocks.event.Event;
import java.time.LocalDate;

/** Pipeline stage boundary for the run keyed on {@code date}. */
public record ZacksBasicCompleteEvent(LocalDate date) implements Event {}

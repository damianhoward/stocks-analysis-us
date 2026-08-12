package io.github.damian1000.stocks.event;

import java.time.LocalDate;

/**
 * A pipeline stage boundary, carrying the trading date the whole run is keyed on.
 *
 * <p>The accessor is {@code date()} rather than {@code getDate()} because every implementation is
 * a record, and a record already generates exactly this method. Naming it {@code getDate()} would
 * oblige each one to add a delegating override — the kind of boilerplate retiring Lombok was
 * meant to remove, reintroduced by hand.
 */
public interface Event {
    LocalDate date();
}

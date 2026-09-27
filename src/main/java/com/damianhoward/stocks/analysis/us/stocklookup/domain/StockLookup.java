package com.damianhoward.stocks.analysis.us.stocklookup.domain;

import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Embedded;
import org.springframework.data.relational.core.mapping.Table;

import java.time.LocalDate;

/**
 * One stock's quote as fetched for a run date, or the reason it could not be. A failed lookup is
 * stored rather than dropped, so the analysis can report the stock as unresolved instead of
 * silently leaving it out.
 */
@Table("stock_lookup")
public record StockLookup(
        @Id String id,
        @Column("date") LocalDate date,
        @Column("zackscode") String zacksCode,
        @Embedded.Empty Quote quote,
        @Column("error_message") String errorMessage) {

    private static final int ERROR_MESSAGE_LENGTH = 200;

    public static StockLookup quoted(LocalDate date, String zacksCode, Quote quote) {
        return new StockLookup(null, date, zacksCode, quote, null);
    }

    /** The message is cut to the column's width, because the reason matters more than its tail. */
    public static StockLookup failed(LocalDate date, String zacksCode, String message) {
        String truncated = message == null || message.length() <= ERROR_MESSAGE_LENGTH
                ? message
                : message.substring(0, ERROR_MESSAGE_LENGTH);
        return new StockLookup(null, date, zacksCode, Quote.NONE, truncated);
    }

    public StockLookup withQuote(Quote replacement) {
        return new StockLookup(id, date, zacksCode, replacement, errorMessage);
    }
}

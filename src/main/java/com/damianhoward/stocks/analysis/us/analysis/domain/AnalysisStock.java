package com.damianhoward.stocks.analysis.us.analysis.domain;

import com.damianhoward.stocks.analysis.us.sectormapping.domain.ZacksSectorMapping;
import com.damianhoward.stocks.analysis.us.stocklookup.domain.Quote;
import com.damianhoward.stocks.analysis.us.stocklookup.domain.StockLookup;
import com.damianhoward.stocks.analysis.us.zackscode.domain.ZacksCode;
import com.damianhoward.stocks.util.Decimals;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Embedded;
import org.springframework.data.relational.core.mapping.Table;

import java.time.LocalDate;

/**
 * One row of the report: a stock's quote, where Zacks classifies it, and its PEG analysis.
 * Ordered by category, then by next year's PEG from highest, which is the report's reading order.
 */
@Table("stock_analysis")
public record AnalysisStock(
        @Id Long id,
        @Column("date") LocalDate date,
        @Column("zackscode") String zacksCode,
        @Column("zackscompany") String zacksCompany,
        @Column("sectorgroup") String sectorGroup,
        @Column("mediumindustrygroup") String mediumIndustryGroup,
        @Column("industry") String industry,
        @Embedded.Empty Quote quote,
        @Embedded.Empty PegRatios ratios,
        @Column("category") String category,
        @Column("error_message") String errorMessage)
        implements Comparable<AnalysisStock> {

    /**
     * Assembles a row from a lookup and its analysis. The Zacks listing and sector mapping are
     * {@code null} when no match was found, which leaves those columns empty rather than failing
     * the stock.
     */
    public static AnalysisStock of(
            LocalDate date, StockLookup lookup, ZacksCode listing, ZacksSectorMapping sector, PEGStock peg) {
        return new AnalysisStock(
                null,
                date,
                lookup.zacksCode(),
                listing == null ? null : listing.company(),
                sector == null ? null : sector.sectorGroup(),
                sector == null ? null : sector.mediumIndustryGroup(),
                sector == null ? null : sector.industry(),
                lookup.quote(),
                peg.ratios().rounded(),
                peg.category(),
                lookup.errorMessage());
    }

    @Override
    public int compareTo(AnalysisStock o) {
        int result = category.compareTo(o.category);
        if (result == 0) {
            result = Decimals.compare(o.ratios.nextYearPEG(), ratios.nextYearPEG());
        }
        return result;
    }
}

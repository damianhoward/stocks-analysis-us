package com.damianhoward.stocks.analysis.us.zackscode.domain;

import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

import java.time.LocalDate;

/** A stock Zacks lists under an industry for a run date, by its ticker and company name. */
@Table("zacks_code")
public record ZacksCode(
        @Id String id,
        @Column("industry") String industry,
        @Column("zackscode") String zacksCode,
        @Column("company") String company,
        @Column("date") LocalDate date) {

    public static ZacksCode of(LocalDate date, String industry, String zacksCode, String company) {
        return new ZacksCode(null, industry, zacksCode, company, date);
    }
}

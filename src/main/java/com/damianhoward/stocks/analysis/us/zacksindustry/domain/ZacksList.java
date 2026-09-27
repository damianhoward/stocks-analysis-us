package com.damianhoward.stocks.analysis.us.zacksindustry.domain;

import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

import java.time.LocalDate;

/** One Zacks industry for a run date: its Zacks index, name and how many stocks it holds. */
@Table("zacks_industry")
public record ZacksList(
        @Id String id,
        @Column("index") String index,
        @Column("total") String total,
        @Column("industry") String industry,
        @Column("date") LocalDate date) {

    public static ZacksList of(LocalDate date, String index, String total, String industry) {
        return new ZacksList(null, index, total, industry, date);
    }
}

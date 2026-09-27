package com.damianhoward.stocks.analysis.us.sectormapping.domain;

import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

import java.time.LocalDate;

/** Where Zacks places an industry in its sector and medium-industry hierarchy, for a run date. */
@Table("zacks_sector_mapping")
public record ZacksSectorMapping(
        @Id String id,
        @Column("sectorgroup") String sectorGroup,
        @Column("mediumindustrygroup") String mediumIndustryGroup,
        @Column("industry") String industry,
        @Column("date") LocalDate date) {

    public static ZacksSectorMapping of(
            LocalDate date, String sectorGroup, String mediumIndustryGroup, String industry) {
        return new ZacksSectorMapping(null, sectorGroup, mediumIndustryGroup, industry, date);
    }
}

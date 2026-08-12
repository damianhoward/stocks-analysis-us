package com.damianhoward.stocks.analysis.us.sectormapping.domain;

import lombok.Data;
import lombok.ToString;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDate;

@Data
@ToString
@Entity
@Table(name = "zacks_sector_mapping")
public class ZacksSectorMapping {

    @Id
    @Column(name = "id")
    private String id;

    @Column(name = "sectorgroup")
    private String sectorGroup;

    @Column(name = "mediumindustrygroup")
    private String mediumIndustryGroup;

    @Column(name = "industry")
    private String industry;

    @Column(name = "date")
    private LocalDate date;
}

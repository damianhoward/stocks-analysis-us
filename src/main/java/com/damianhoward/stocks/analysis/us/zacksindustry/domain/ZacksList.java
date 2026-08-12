package com.damianhoward.stocks.analysis.us.zacksindustry.domain;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDate;

@Data
@EqualsAndHashCode
@ToString
@Entity
@Table(name = "zacks_industry")
public class ZacksList {

    @Id
    @Column(name = "id")
    private String id;

    @Column(name = "index")
    private String index;

    @Column(name = "total")
    private String total;

    @Column(name = "industry")
    private String industry;

    @Column(name = "date")
    private LocalDate date;
}

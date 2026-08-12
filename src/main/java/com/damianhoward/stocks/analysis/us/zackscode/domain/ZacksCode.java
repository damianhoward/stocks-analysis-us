package com.damianhoward.stocks.analysis.us.zackscode.domain;

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
@Table(name = "zacks_code")
public class ZacksCode {

    @Id
    @Column(name = "id")
    private String id;

    @Column(name = "industry")
    private String industry;

    @Column(name = "zackscode")
    private String zacksCode;

    @Column(name = "company")
    private String company;

    @Column(name = "date")
    private LocalDate date;
}

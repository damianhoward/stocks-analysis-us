package com.damianhoward.stocks.analysis.us.stocklookup.domain;

import lombok.*;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@EqualsAndHashCode
@Builder(toBuilder = true)
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "stock_lookup")
public class StockLookup {

    @Id
    private String id;

    private LocalDate date;

    @Column(name = "zackscode")
    private String zacksCode;

    private String company;

    private String currency;

    @Column(name = "market_cap")
    private BigDecimal marketCap;

    @Column(name = "year_ending")
    private String yearEnding;

    private BigDecimal beta;

    private BigDecimal price;

    @Column(name = "target_price")
    private BigDecimal targetPrice;

    @Column(name = "last_year_eps")
    private BigDecimal lastYearEPS;

    @Column(name = "last_year_pe")
    private BigDecimal lastYearPE;

    @Column(name = "this_year_estimate_eps")
    private BigDecimal thisYearEstimateEPS;

    @Column(name = "next_year_estimate_eps")
    private BigDecimal nextYearEstimateEPS;

    @Column(name = "earning_above_estimates")
    private String earningAboveEstimates;

    @Column(name = "recommendation_rating")
    private BigDecimal recommendationRating;

    @Column(name = "error_message")
    private String errorMessage;

    public boolean isValid() {
        return price != null &&
               lastYearEPS != null &&
               thisYearEstimateEPS != null &&
               nextYearEstimateEPS != null;
    }

}

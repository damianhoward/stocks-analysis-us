package com.damianhoward.stocks.analysis.us.analysis.domain;

import com.damianhoward.stocks.util.Decimals;
import lombok.*;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@EqualsAndHashCode
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "stock_analysis")
public class AnalysisStock implements Comparable<AnalysisStock> {

    @Id
    @GeneratedValue(generator = "stock_analysis_generator")
    @SequenceGenerator(name = "stock_analysis_generator", sequenceName = "stock_analysis_sequence", allocationSize = 500)
    private Long id;

    private LocalDate date;

    @Column(name = "sectorgroup")
    private String sectorGroup;

    @Column(name = "mediumindustrygroup")
    private String mediumIndustryGroup;

    @Column(name = "industry")
    private String industry;

    @Column(name = "zackscode")
    private String zacksCode;

    @Column(name = "zackscompany")
    private String zacksCompany;

    // StockLookup
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

    // PEG Stock
    @Column(name = "this_year_estimate_pe")
    private BigDecimal thisYearEstimatePE;

    @Column(name = "next_year_estimate_pe")
    private BigDecimal nextYearEstimatePE;

    @Column(name = "this_year_eps_growth")
    private BigDecimal thisYearEPSGrowth;

    @Column(name = "next_year_eps_growth")
    private BigDecimal nextYearEPSGrowth;

    @Column(name = "this_year_peg")
    private BigDecimal thisYearPEG;

    @Column(name = "next_year_peg")
    private BigDecimal nextYearPEG;

    @Column(name = "error_message")
    private String errorMessage;

    @Column(name = "category")
    private String category;

    @Override
    public int compareTo(AnalysisStock o) {
        int result = category.compareTo(o.category);
        if (result == 0) {
            result = Decimals.compare(o.nextYearPEG, nextYearPEG);
        }
        return result;
    }

}

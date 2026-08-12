package com.damianhoward.stocks.analysis.us.stocklookup.service.yahoo;

import java.math.BigDecimal;
import java.util.List;

/**
 * Top-level shape of the Yahoo {@code quoteSummary} JSON API response:
 * {@code {"quoteSummary":{"result":[ <store> ],"error":{...}}}}.
 *
 * <p>These are records rather than mutable beans because nothing here is ever populated by hand:
 * Gson builds them from the response through the canonical constructor and the pipeline only
 * reads them. Every component is a reference type on purpose — a field Yahoo omits arrives as
 * null, and the reader below distinguishes "absent" from "zero" for all of them. A primitive
 * would silently turn a missing figure into a real one.
 */
public record QuoteSummary(QuoteSummaryResult quoteSummary) {}

record QuoteSummaryResult(List<QuoteSummaryStore> result, QuoteSummaryError error) {}

record QuoteSummaryError(String code, String description) {}

record QuoteSummaryStore(
        RecommendationTrends recommendationTrend,
        Price price,
        SummaryDetail summaryDetail,
        FinancialData financialData,
        EarningsTrends earningsTrend,
        EarningsHistory earningsHistory) {}

record EarningsHistory(List<History> history) {}

record History(Raw epsDifference) {}

record EarningsTrends(List<EarningTrend> trend) {}

record EarningTrend(Long maxAge, String period, String endDate, EarningsEstimate earningsEstimate) {}

record EarningsEstimate(Raw avg, Raw yearAgoEps) {}

record Raw(BigDecimal raw) {}

record FinancialData(Raw targetMeanPrice, String financialCurrency, Raw recommendationMean) {}

record SummaryDetail(Raw previousClose, Raw beta, String currency, Raw trailingPE) {}

record Price(Raw marketCap, String currency, String longName) {}

record RecommendationTrends(List<RecommendationTrend> trend) {}

record RecommendationTrend(
        String period, Long strongBuy, Long buy, Long hold, Long sell, Long strongSell) {}

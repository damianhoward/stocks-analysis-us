package com.damianhoward.stocks.analysis.us.stocklookup.service.yahoo;

import com.google.gson.Gson;
import com.damianhoward.stocks.analysis.us.stocklookup.domain.Quote;
import com.damianhoward.stocks.exception.DataRetrievalError;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.List;
import java.util.Objects;

@Component
public class YahooStockLookup {

    private final YahooFinanceClient yahooFinanceClient;

    public YahooStockLookup(YahooFinanceClient yahooFinanceClient) {
        this.yahooFinanceClient = yahooFinanceClient;
    }

    public Quote lookup(String zacksCode) throws DataRetrievalError {
        // Yahoo writes share classes without the dot Zacks uses (BRK.B is BRKB).
        String symbol = zacksCode.replaceAll("\\.", "");

        String json = yahooFinanceClient.fetchQuoteSummary(symbol);
        QuoteSummary quoteSummary = new Gson().fromJson(json, QuoteSummary.class);
        if (quoteSummary == null
                || quoteSummary.quoteSummary() == null
                || quoteSummary.quoteSummary().result() == null
                || quoteSummary.quoteSummary().result().isEmpty()) {
            throw new DataRetrievalError(String.format(
                    "Yahoo response for %s contained no quoteSummary result — symbol may be unknown or the API changed",
                    symbol));
        }
        QuoteSummaryStore store = quoteSummary.quoteSummary().result().get(0);

        Price price = store.price();
        SummaryDetail summary = store.summaryDetail();
        FinancialData financials = store.financialData();
        EarningsEstimate thisYear = estimateFor(store.earningsTrend(), "0y");
        EarningsEstimate nextYear = estimateFor(store.earningsTrend(), "+1y");

        return new Quote(
                price == null ? null : price.longName(),
                price == null ? null : price.currency(),
                price == null ? null : raw(price.marketCap()),
                null,
                summary == null ? null : raw(summary.beta()),
                // A close with no currency is a number without a unit, so it is not taken.
                summary == null || summary.currency() == null ? null : raw(summary.previousClose()),
                financials == null ? null : raw(financials.targetMeanPrice()),
                thisYear == null ? null : raw(thisYear.yearAgoEps()),
                summary == null ? null : raw(summary.trailingPE()),
                thisYear == null ? null : raw(thisYear.avg()),
                nextYear == null ? null : raw(nextYear.avg()),
                earningsAboveEstimates(store.earningsHistory()),
                financials == null ? null : raw(financials.recommendationMean()));
    }

    private static EarningsEstimate estimateFor(EarningsTrends trends, String period) {
        if (trends == null || trends.trend() == null) {
            return null;
        }
        return trends.trend().stream()
                .filter(Objects::nonNull)
                .filter(trend -> period.equalsIgnoreCase(trend.period()))
                .map(EarningTrend::earningsEstimate)
                .filter(Objects::nonNull)
                .reduce((first, later) -> later)
                .orElse(null);
    }

    /** How many reported quarters beat the EPS estimate, out of those with a reported difference. */
    private static String earningsAboveEstimates(EarningsHistory history) {
        if (history == null || history.history() == null) {
            return null;
        }
        List<BigDecimal> differences = history.history().stream()
                .filter(Objects::nonNull)
                .map(History::epsDifference)
                .filter(Objects::nonNull)
                .map(Raw::raw)
                .toList();
        long above = differences.stream()
                .filter(diff -> diff != null && diff.compareTo(BigDecimal.ZERO) > 0)
                .count();
        return String.format("%s out of %s above estimated eps", above, differences.size());
    }

    private static BigDecimal raw(Raw value) {
        return value == null ? null : value.raw();
    }
}

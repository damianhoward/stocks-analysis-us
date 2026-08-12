package io.github.damian1000.stocks.analysis.us.stocklookup.service.yahoo;

import com.google.gson.Gson;
import io.github.damian1000.stocks.analysis.us.stocklookup.domain.StockLookup;
import io.github.damian1000.stocks.exception.DataRetrievalError;
import io.github.damian1000.stocks.util.IdGenerator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Component
public class YahooStockLookup {

    private static final Logger log = LoggerFactory.getLogger(YahooStockLookup.class);

    private final YahooFinanceClient yahooFinanceClient;

    public YahooStockLookup(YahooFinanceClient yahooFinanceClient) {
        this.yahooFinanceClient = yahooFinanceClient;
    }

    public StockLookup lookup(String zacksCode) throws DataRetrievalError {
        zacksCode = zacksCode.replaceAll("\\.", "");

        StockLookup stockLookup = new StockLookup();
        stockLookup.setId(IdGenerator.generateId());
        stockLookup.setDate(LocalDate.now());
        stockLookup.setZacksCode(zacksCode);

        String json = yahooFinanceClient.fetchQuoteSummary(zacksCode);
        QuoteSummary quoteSummary = new Gson().fromJson(json, QuoteSummary.class);
        if (quoteSummary == null
                || quoteSummary.quoteSummary() == null
                || quoteSummary.quoteSummary().result() == null
                || quoteSummary.quoteSummary().result().isEmpty()) {
            throw new DataRetrievalError(String.format(
                    "Yahoo response for %s contained no quoteSummary result — symbol may be unknown or the API changed",
                    zacksCode));
        }
        {

            QuoteSummaryStore quoteSummaryStore = quoteSummary.quoteSummary().result().get(0);

            Price price = quoteSummaryStore.price();
            if (price != null) {
                stockLookup.setCurrency(price.currency());
                Raw priceMarketCap = price.marketCap();
                if (priceMarketCap != null) {
                    stockLookup.setMarketCap(priceMarketCap.raw());
                }
                stockLookup.setCompany(price.longName());
            }

            SummaryDetail summaryDetail = quoteSummaryStore.summaryDetail();
            if (summaryDetail != null) {
                Raw betaSummary = summaryDetail.beta();
                if (betaSummary != null) {
                    stockLookup.setBeta(betaSummary.raw());
                }

                String currency = summaryDetail.currency();
                Raw previousClose = summaryDetail.previousClose();
                if (previousClose != null) {
                    BigDecimal raw = previousClose.raw();
                    if (currency != null && raw != null) {
                        stockLookup.setPrice(raw);
                    }
                }

                Raw trailingPE = summaryDetail.trailingPE();
                if (trailingPE != null) {
                    stockLookup.setLastYearPE(trailingPE.raw());
                }
            }

            FinancialData financialData = quoteSummaryStore.financialData();
            if (financialData != null) {
                Raw targetMeanPrice = financialData.targetMeanPrice();
                if (targetMeanPrice != null) {
                    stockLookup.setTargetPrice(targetMeanPrice.raw());
                }
                Raw recommendationMean = financialData.recommendationMean();
                if (recommendationMean != null) {
                    stockLookup.setRecommendationRating(recommendationMean.raw());
                }
            }

            EarningsTrends earningsTrend = quoteSummaryStore.earningsTrend();
            if (earningsTrend != null) {
                List<EarningTrend> earningsTrendList = earningsTrend.trend();
                if (earningsTrendList != null) {
                    for (EarningTrend trend : earningsTrendList) {
                        if (trend != null) {
                            if ("0y".equalsIgnoreCase(trend.period())) {
                                EarningsEstimate earningsEstimate = trend.earningsEstimate();
                                if (earningsEstimate != null) {
                                    Raw average = earningsEstimate.avg();
                                    if (average != null) {
                                        stockLookup.setThisYearEstimateEPS(average.raw());
                                    }

                                    Raw yearAgoEps = earningsEstimate.yearAgoEps();
                                    if (yearAgoEps != null) {
                                        stockLookup.setLastYearEPS(yearAgoEps.raw());
                                    }
                                }

                            }
                            if ("+1y".equalsIgnoreCase(trend.period())) {
                                EarningsEstimate earningsEstimate = trend.earningsEstimate();
                                if (earningsEstimate != null) {
                                    Raw average = earningsEstimate.avg();
                                    if (average != null) {
                                        stockLookup.setNextYearEstimateEPS(average.raw());
                                    }
                                }
                            }
                        }
                    }
                }

            }

            EarningsHistory earningsHistory = quoteSummaryStore.earningsHistory();
            if (earningsHistory != null) {
                List<History> historyList = earningsHistory.history();
                if (historyList != null) {
                    int numberOfHistoryRecords = 0;
                    int aboveEstimatedEps = 0;
                    for (History history: historyList) {
                        if (history != null) {
                            Raw epsDifference = history.epsDifference();
                            if (epsDifference != null) {
                                numberOfHistoryRecords++;
                                BigDecimal diff = epsDifference.raw();
                                if (diff != null && diff.compareTo(BigDecimal.ZERO) > 0) {
                                    aboveEstimatedEps++;
                                }
                            }
                        }
                    }
                    stockLookup.setEarningAboveEstimates(String.format("%s out of %s above estimated eps",
                            aboveEstimatedEps, numberOfHistoryRecords));
                }
            }

        }
        return stockLookup;
    }

}

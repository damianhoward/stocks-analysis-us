package com.damianhoward.stocks.analysis.us.stocklookup.service.yahoo;

import com.damianhoward.stocks.analysis.us.stocklookup.domain.StockLookup;
import com.damianhoward.stocks.exception.DataRetrievalError;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Parses Yahoo quoteSummary JSON (as returned by {@link YahooFinanceClient}) into a StockLookup.
 * The client is mocked so these tests cover only the mapping of the API's module structure.
 */
class YahooStockLookupParseTest {

    private YahooFinanceClient yahooFinanceClient;
    private YahooStockLookup yahooStockLookup;

    @BeforeEach
    void setUp() {
        yahooFinanceClient = mock(YahooFinanceClient.class);
        yahooStockLookup = new YahooStockLookup(yahooFinanceClient);
    }

    private static String envelope(String store) {
        return "{\"quoteSummary\":{\"result\":[" + store + "],\"error\":null}}";
    }

    @Test
    void parsesPriceSummaryFinancialEarningsAndHistory() throws DataRetrievalError {
        String store =
                "{" +
                "\"price\":{\"marketCap\":{\"raw\":1000},\"currency\":\"USD\",\"longName\":\"Acme Inc\"}," +
                "\"summaryDetail\":{\"previousClose\":{\"raw\":100.5},\"beta\":{\"raw\":1.2}," +
                "  \"currency\":\"USD\",\"trailingPE\":{\"raw\":15.7}}," +
                "\"financialData\":{\"targetMeanPrice\":{\"raw\":120.3},\"recommendationMean\":{\"raw\":2.5}}," +
                "\"earningsTrend\":{\"trend\":[" +
                "  {\"period\":\"0y\",\"earningsEstimate\":{\"avg\":{\"raw\":5.1},\"yearAgoEps\":{\"raw\":4.0}}}," +
                "  {\"period\":\"+1y\",\"earningsEstimate\":{\"avg\":{\"raw\":6.2}}}," +
                "  {\"period\":\"+5y\"}]}," +
                "\"earningsHistory\":{\"history\":[" +
                "  {\"epsDifference\":{\"raw\":0.5}}," +
                "  {\"epsDifference\":{\"raw\":-0.2}}," +
                "  {\"epsDifference\":{\"raw\":0.3}}]}}";
        when(yahooFinanceClient.fetchQuoteSummary(anyString())).thenReturn(envelope(store));

        StockLookup result = yahooStockLookup.lookup("ACME.O");

        // replaceAll("\\.", "") strips the dot but keeps the suffix character.
        assertEquals("ACMEO", result.getZacksCode());
        assertEquals("Acme Inc", result.getCompany());
        assertEquals(new BigDecimal("1000"), result.getMarketCap());
        assertEquals("USD", result.getCurrency());

        assertEquals(new BigDecimal("100.5"), result.getPrice());
        assertEquals(new BigDecimal("1.2"), result.getBeta());
        assertEquals(new BigDecimal("15.7"), result.getLastYearPE());

        assertEquals(new BigDecimal("120.3"), result.getTargetPrice());
        assertEquals(new BigDecimal("2.5"), result.getRecommendationRating());

        assertEquals(new BigDecimal("5.1"), result.getThisYearEstimateEPS());
        assertEquals(new BigDecimal("4.0"), result.getLastYearEPS());
        assertEquals(new BigDecimal("6.2"), result.getNextYearEstimateEPS());

        // 2 of 3 history entries are above zero
        assertEquals("2 out of 3 above estimated eps", result.getEarningAboveEstimates());

        assertNotNull(result.getId());
        assertNotNull(result.getDate());
    }

    @Test
    void storeWithOnlyEmptyPriceLeavesAllFieldsNull() throws DataRetrievalError {
        when(yahooFinanceClient.fetchQuoteSummary(anyString())).thenReturn(envelope("{\"price\":{}}"));

        StockLookup result = yahooStockLookup.lookup("BLNK");

        assertEquals("BLNK", result.getZacksCode());
        assertNull(result.getPrice());
        assertNull(result.getBeta());
        assertNull(result.getCompany());
    }

    @Test
    void presentSectionsWithEmptyInnerObjectsLeaveFieldsUnset() throws DataRetrievalError {
        when(yahooFinanceClient.fetchQuoteSummary(anyString())).thenReturn(envelope(
                "{\"price\":{},\"summaryDetail\":{},\"financialData\":{}," +
                "\"earningsTrend\":{\"trend\":[]},\"earningsHistory\":{\"history\":[]}}"));

        StockLookup result = yahooStockLookup.lookup("EMPTY");

        assertNull(result.getMarketCap());
        assertNull(result.getPrice());
        assertNull(result.getTargetPrice());
        assertNull(result.getRecommendationRating());
        assertEquals("0 out of 0 above estimated eps", result.getEarningAboveEstimates());
    }

    @Test
    void trendAndHistoryEntriesWithNullInnerValuesAreSkipped() throws DataRetrievalError {
        when(yahooFinanceClient.fetchQuoteSummary(anyString())).thenReturn(envelope(
                "{\"summaryDetail\":{\"previousClose\":{\"raw\":50}}," +
                "\"earningsTrend\":{\"trend\":[" +
                "  {\"period\":\"0y\",\"earningsEstimate\":{}}," +
                "  {\"period\":\"+1y\",\"earningsEstimate\":{}}," +
                "  {\"period\":\"0y\",\"earningsEstimate\":null}," +
                "  null]}," +
                "\"earningsHistory\":{\"history\":[{\"epsDifference\":null},{\"epsDifference\":{}},null]}}"));

        StockLookup result = yahooStockLookup.lookup("NULLS");

        // previousClose present but no currency -> price stays unset
        assertNull(result.getPrice());
        assertNull(result.getThisYearEstimateEPS());
        assertNull(result.getNextYearEstimateEPS());
        assertNull(result.getLastYearEPS());
        assertEquals("0 out of 1 above estimated eps", result.getEarningAboveEstimates());
    }

    @Test
    void emptyResultListThrows() throws DataRetrievalError {
        when(yahooFinanceClient.fetchQuoteSummary(anyString()))
                .thenReturn("{\"quoteSummary\":{\"result\":[],\"error\":null}}");

        assertThrows(DataRetrievalError.class, () -> yahooStockLookup.lookup("GONE"));
    }

    @Test
    void nullOrIncompleteEnvelopeThrows() throws DataRetrievalError {
        // Each variant trips a different link in the null-guard chain:
        // null whole payload, missing quoteSummary, and missing result list.
        for (String json : new String[] {"null", "{}", "{\"quoteSummary\":{}}"}) {
            when(yahooFinanceClient.fetchQuoteSummary(anyString())).thenReturn(json);
            assertThrows(DataRetrievalError.class, () -> yahooStockLookup.lookup("X"));
        }
    }

    @Test
    void currencyWithoutCloseAndNullTrendOrHistoryAreHandled() throws DataRetrievalError {
        // currency present but previousClose has no raw value -> price stays unset;
        // earningsTrend/earningsHistory present but with null inner lists.
        when(yahooFinanceClient.fetchQuoteSummary(anyString())).thenReturn(envelope(
                "{\"summaryDetail\":{\"currency\":\"USD\",\"previousClose\":{}},"
                        + "\"earningsTrend\":{},\"earningsHistory\":{}}"));

        StockLookup result = yahooStockLookup.lookup("PARTIAL");

        assertNull(result.getPrice());
        assertNull(result.getEarningAboveEstimates());
    }

    @Test
    void plusOneYearTrendWithNullEstimateIsSkipped() throws DataRetrievalError {
        when(yahooFinanceClient.fetchQuoteSummary(anyString())).thenReturn(envelope(
                "{\"earningsTrend\":{\"trend\":[{\"period\":\"+1y\",\"earningsEstimate\":null}]}}"));

        StockLookup result = yahooStockLookup.lookup("PLUS1");

        assertNull(result.getNextYearEstimateEPS());
    }
}

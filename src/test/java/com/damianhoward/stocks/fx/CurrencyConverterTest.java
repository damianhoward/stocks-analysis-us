package com.damianhoward.stocks.fx;

import com.damianhoward.stocks.exception.DataRetrievalError;
import com.damianhoward.stocks.html.HtmlRetriever;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.math.MathContext;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class CurrencyConverterTest {

    private static final String PROVIDER_URL = "https://example.test/fx";

    private final HtmlRetriever retriever = mock(HtmlRetriever.class);
    private final CurrencyConverter converter = new CurrencyConverter(retriever, PROVIDER_URL);

    @Test
    void convertsByDividingTheTwoEurLegs() throws DataRetrievalError {
        when(retriever.getHtml(contains("quotes=GBP,USD"))).thenReturn(
                "[{\"date\":\"2026-06-27\",\"base\":\"EUR\",\"quote\":\"GBP\",\"rate\":0.86254},"
                        + "{\"date\":\"2026-06-27\",\"base\":\"EUR\",\"quote\":\"USD\",\"rate\":1.1392}]");

        // 1 GBP buys (EUR->USD) / (EUR->GBP) USD, computed from the decimals as written.
        BigDecimal expected = new BigDecimal("1.1392").divide(new BigDecimal("0.86254"), MathContext.DECIMAL64);
        assertEquals(Optional.of(expected), converter.convert("GBP", "USD"));
    }

    @Test
    void treatsEurAsTheBaseWithRateOne() throws DataRetrievalError {
        when(retriever.getHtml(contains("quotes=EUR,USD"))).thenReturn(
                "[{\"date\":\"2026-06-27\",\"base\":\"EUR\",\"quote\":\"USD\",\"rate\":1.1392}]");

        assertEquals(0, new BigDecimal("1.1392").compareTo(converter.convert("EUR", "USD").orElseThrow()));
    }

    @Test
    void cachesSoTheSamePairIsFetchedOnce() throws DataRetrievalError {
        when(retriever.getHtml(contains("quotes=AUD,NZD"))).thenReturn(
                "[{\"date\":\"2026-06-27\",\"base\":\"EUR\",\"quote\":\"AUD\",\"rate\":1.65},"
                        + "{\"date\":\"2026-06-27\",\"base\":\"EUR\",\"quote\":\"NZD\",\"rate\":2.017}]");

        Optional<BigDecimal> first = converter.convert("AUD", "NZD");
        Optional<BigDecimal> second = converter.convert("AUD", "NZD");

        assertEquals(first, second);
        verify(retriever, times(1)).getHtml(anyString());
    }

    @Test
    void hasNoRateWhenTheResponseOmitsOneLeg() throws DataRetrievalError {
        when(retriever.getHtml(contains("quotes=GBP,ZZZ"))).thenReturn(
                "[{\"date\":\"2026-06-27\",\"base\":\"EUR\",\"quote\":\"GBP\",\"rate\":0.86254}]");

        assertEquals(Optional.empty(), converter.convert("GBP", "ZZZ"));
    }

    @Test
    void hasNoRateForAnInvalidCurrencyCodeAndDoesNotAsk() throws DataRetrievalError {
        assertEquals(Optional.empty(), converter.convert("GB", "USD"));
        assertEquals(Optional.empty(), converter.convert("", "USD"));
        assertEquals(Optional.empty(), converter.convert(null, "USD"));
        verify(retriever, never()).getHtml(anyString());
    }

    @Test
    void aResponseThatIsNotJsonIsARetrievalErrorNotAMissingRate() throws DataRetrievalError {
        when(retriever.getHtml(contains("quotes=GBP,USD"))).thenReturn("<html>maintenance</html>");

        assertThrows(DataRetrievalError.class, () -> converter.convert("GBP", "USD"));
    }
}

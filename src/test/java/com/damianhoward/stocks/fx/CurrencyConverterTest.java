package com.damianhoward.stocks.fx;

import com.damianhoward.stocks.exception.DataRetrievalError;
import com.damianhoward.stocks.html.HtmlParser;
import com.damianhoward.stocks.html.HtmlResponse;
import com.damianhoward.stocks.html.HtmlRetriever;
import org.junit.jupiter.api.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.closeTo;
import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class CurrencyConverterTest {

    private static final String PROVIDER_URL = "https://example.test/fx";

    private static HtmlResponse response(String rawHtml) {
        HtmlResponse response = new HtmlResponse();
        response.rawHtml = rawHtml;
        return response;
    }

    @Test
    void convertsByDividingTheTwoEurLegs() throws DataRetrievalError {
        HtmlRetriever retriever = mock(HtmlRetriever.class);
        when(retriever.getHtml(contains("quotes=GBP,USD"))).thenReturn(response(
                "[{\"date\":\"2026-06-27\",\"base\":\"EUR\",\"quote\":\"GBP\",\"rate\":0.86254},"
                        + "{\"date\":\"2026-06-27\",\"base\":\"EUR\",\"quote\":\"USD\",\"rate\":1.1392}]"));

        CurrencyConverter converter = new CurrencyConverter(retriever, new HtmlParser(), PROVIDER_URL);

        // 1 GBP buys (EUR->USD) / (EUR->GBP) USD.
        assertThat(converter.convert("GBP", "USD"), is(closeTo(1.1392 / 0.86254, 0.0001)));
    }

    @Test
    void treatsEurAsTheBaseWithRateOne() throws DataRetrievalError {
        HtmlRetriever retriever = mock(HtmlRetriever.class);
        when(retriever.getHtml(contains("quotes=EUR,USD"))).thenReturn(response(
                "[{\"date\":\"2026-06-27\",\"base\":\"EUR\",\"quote\":\"USD\",\"rate\":1.1392}]"));

        CurrencyConverter converter = new CurrencyConverter(retriever, new HtmlParser(), PROVIDER_URL);

        assertThat(converter.convert("EUR", "USD"), is(closeTo(1.1392, 0.0001)));
    }

    @Test
    void cachesSoTheSamePairIsFetchedOnce() throws DataRetrievalError {
        HtmlRetriever retriever = mock(HtmlRetriever.class);
        when(retriever.getHtml(contains("quotes=AUD,NZD"))).thenReturn(response(
                "[{\"date\":\"2026-06-27\",\"base\":\"EUR\",\"quote\":\"AUD\",\"rate\":1.65},"
                        + "{\"date\":\"2026-06-27\",\"base\":\"EUR\",\"quote\":\"NZD\",\"rate\":2.017}]"));

        CurrencyConverter converter = new CurrencyConverter(retriever, new HtmlParser(), PROVIDER_URL);

        double first = converter.convert("AUD", "NZD");
        double second = converter.convert("AUD", "NZD");

        assertThat(second, is(first));
        verify(retriever, times(1)).getHtml(org.mockito.ArgumentMatchers.anyString());
    }

    @Test
    void returnsZeroWhenAResponseRateIsMissing() throws DataRetrievalError {
        HtmlRetriever retriever = mock(HtmlRetriever.class);
        when(retriever.getHtml(contains("quotes=GBP,ZZZ"))).thenReturn(response(
                "[{\"date\":\"2026-06-27\",\"base\":\"EUR\",\"quote\":\"GBP\",\"rate\":0.86254}]"));

        CurrencyConverter converter = new CurrencyConverter(retriever, new HtmlParser(), PROVIDER_URL);

        assertThat(converter.convert("GBP", "ZZZ"), is(0.0));
    }

    @Test
    void returnsZeroForInvalidCurrencyCode() throws DataRetrievalError {
        HtmlRetriever retriever = mock(HtmlRetriever.class);
        CurrencyConverter converter = new CurrencyConverter(retriever, new HtmlParser(), PROVIDER_URL);

        assertThat(converter.convert("GB", "USD"), is(0.0));
        assertThat(converter.convert("", "USD"), is(0.0));
        verify(retriever, never()).getHtml(org.mockito.ArgumentMatchers.anyString());
    }
}

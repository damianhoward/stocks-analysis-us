package com.damianhoward.stocks.html;

import com.damianhoward.stocks.domain.Amount;
import org.apache.commons.lang3.math.NumberUtils;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import static com.damianhoward.stocks.util.Delimiter.LINE_BREAK;
import static com.damianhoward.stocks.util.Delimiter.TAB;

@Component
public class HtmlParser {

    public String extractText(String html, String startText, String endText) {
        int startIndex = html.indexOf(startText) + startText.length();
        int endIndex = html.indexOf(endText, startIndex);
        if (startIndex > 0 && endIndex > 0) {
            return html.substring(startIndex, endIndex);
        }
        return "";
    }

    public String extractTextFromStartString(String html, String startString, String startText, String endText) {
        int index = html.indexOf(startString);
        int startIndex = html.indexOf(startText, index) + startText.length();
        int endIndex = html.indexOf(endText, startIndex);
        if (startIndex > 0 && endIndex > 0) {
            return html.substring(index, endIndex);
        }
        return "";
    }

    public String extractText(String html, String startString, String startText, String endText) {
        int index = html.indexOf(startString);
        int startIndex = html.indexOf(startText, index) + startText.length();
        int endIndex = html.indexOf(endText, startIndex);
        if (startIndex > 0 && endIndex > 0) {
            return html.substring(startIndex, endIndex);
        }
        return "";
    }

    public String extractRow(String html, String startString, String endString) {
        int index = html.indexOf(startString);
        int endIndex = html.indexOf(endString, index);
        if (index > 0 && endIndex > 0) {
            return html.substring(index, endIndex);
        }
        return "";
    }

    public String extractLine(String html, String lineContents) {
        String[] lines = html.split("\n");
        for (String line:lines) {
            if (line.contains(lineContents)) {
                return line.replaceAll("\t", "");
            }
        }
        return "";
    }

    public String extractRow(String html, String startString) {
        int index = html.indexOf(startString);
        if (index > 0) {
            return html.substring(index);
        }
        return "";
    }

    public double extractCellValue(String html, String startString, int column) {
        String value = extractCell(html, startString, column);
        if (NumberUtils.isNumber(value)) {
            return Double.valueOf(value);
        }
        return 0.0;
    }

    public String extractCell(String html, String startString, int column) {
        List<String> results = extractCells(html, startString, column, column+1);
        if (!results.isEmpty()) {
            return results.get(0);
        }
        return "";
    }

    public List<String> extractCells(String html, String startString, int from, int to) {
        List<String> results = new ArrayList<>();
        int startIndex = html.indexOf(startString);
        if (startIndex >= 0) {
            String row = html.substring(startIndex+startString.length());
            // remove leading non string characters, replace line breaks with tabs and any duplicate tabs
            row = row.replaceAll("^\\s+", "").replaceAll(LINE_BREAK, TAB).replaceAll(TAB+TAB, TAB);
            String[] cells = row.split(TAB);
            for (int index=from; index<=to; index++) {
                if  (index < cells.length) {
                    String cell = cells[index].trim();
                    results.add(cell);
                }
            }
        }
        return results;
    }

    public double addResults(List<String> results) {
        double resultAsDouble = 0.0;
        for (String result : results) {
            if (NumberUtils.isNumber(result)) {
                resultAsDouble += Double.valueOf(result);
            }
        }
        return resultAsDouble;
    }

    public Amount extractCurrencyAndNumber(String rawPrice) {
        StringBuilder currency = new StringBuilder();
        StringBuilder price = new StringBuilder();

        // handle kr. special case
        if (rawPrice.startsWith("kr.")) {
            currency.append("kr.");
            rawPrice = rawPrice.substring(3);
        }

        for (int i = 0; i < rawPrice.length(); i++ ) {
            char ch = rawPrice.charAt(i);
            if (Character.isDigit(ch) || ch == '.') {
                price.append(ch);
            } else if (ch != ',') {
                currency.append(ch);
            }
        }

        if (price.toString().isEmpty() || price.toString().equals(".")) {
            return new Amount(currency.toString(), BigDecimal.ZERO);
        } else {
            return new Amount(currency.toString(),  new BigDecimal(price.toString()));
        }
    }
}

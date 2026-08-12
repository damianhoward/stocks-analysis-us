package com.damianhoward.stocks.html;

import com.damianhoward.stocks.domain.Amount;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class HtmlParserTest {

    private final HtmlParser htmlParser = new HtmlParser();

    @Test
    void extractText_returnsSubstringBetweenMarkers() {
        String html = "{\"RYN\":{\"exchange\":\"Real Time Quote from BATS\",\"x\":\"y\"}";
        assertEquals("Real Time Quote from BATS", htmlParser.extractText(html, "exchange\":\"", "\",\""));
    }

    @Test
    void extractText_returnsEmptyWhenMarkersMissing() {
        assertEquals("", htmlParser.extractText("nothing here", "missing", "marker"));
    }

    @Test
    void extractTextWithStartString_findsLaterOccurrenceOfStartText() {
        String html = "{\"RYN\":{\"exchange\":\"BATS\",\"x\":\"y\",\"exchange\":\"NYSE\",\"z\":\"q\"}";
        assertEquals("NYSE", htmlParser.extractText(html, "x", "exchange\":\"", "\",\""));
    }

    @Test
    void extractTextFromStartString_includesStartStringInResult() {
        String html = "prefix XSTART_keep_this_part END_other_part";
        String result = htmlParser.extractTextFromStartString(html, "XSTART", "_", "END");
        assertTrue(result.startsWith("XSTART"));
        assertTrue(result.contains("keep_this_part"));
    }

    @Test
    void extractRow_singleArg_takesEverythingFromStartStringOnwards() {
        String html = "prefix\nmiddle\nstart_marker rest of row";
        String row = htmlParser.extractRow(html, "start_marker");
        assertTrue(row.startsWith("start_marker"));
        assertTrue(row.endsWith("rest of row"));
    }

    @Test
    void extractRow_doubleArg_takesBetweenMarkers() {
        String html = "before BEGIN payload END after";
        assertEquals("BEGIN payload ", htmlParser.extractRow(html, "BEGIN", "END"));
    }

    @Test
    void extractRow_returnsEmptyWhenStartMissing() {
        assertEquals("", htmlParser.extractRow("nothing useful here", "missing"));
    }

    @Test
    void extractLine_returnsFirstLineContainingTheTextWithTabsStripped() {
        String html = "header line\n\tindented\tneedle\tline\nfooter";
        assertEquals("indentedneedleline", htmlParser.extractLine(html, "needle"));
    }

    @Test
    void extractLine_returnsEmptyWhenNoLineMatches() {
        assertEquals("", htmlParser.extractLine("line1\nline2", "missing"));
    }

    @Test
    void extractCells_walksTabSeparatedFieldsAfterTheMarker() {
        // After "Stats:" we expect tab-separated 1, 2, 3, 4
        String html = "Stats:\t1\t2\t3\t4\nnextrow";
        List<String> cells = htmlParser.extractCells(html, "Stats:", 0, 2);
        assertEquals(List.of("1", "2", "3"), cells);
    }

    @Test
    void extractCell_returnsTheRequestedColumnOrEmpty() {
        String html = "Stats:\t1\t2\t3\t4\n";
        assertEquals("2", htmlParser.extractCell(html, "Stats:", 1));
        assertEquals("", htmlParser.extractCell("no marker here", "Stats:", 0));
    }

    @Test
    void extractCellValue_parsesNumericCellOrZero() {
        String html = "Stats:\t1.5\tNOT_A_NUMBER\t\n";
        assertEquals(1.5, htmlParser.extractCellValue(html, "Stats:", 0));
        assertEquals(0.0, htmlParser.extractCellValue(html, "Stats:", 1));
    }

    @Test
    void addResults_sumsParseableValuesIgnoringJunk() {
        assertEquals(6.0, htmlParser.addResults(List.of("1", "2", "junk", "3")));
        assertEquals(0.0, htmlParser.addResults(List.of()));
    }

    @Test
    void extractCurrencyAndNumber_extractsSymbolAndDigits() {
        Amount a = htmlParser.extractCurrencyAndNumber("$1,234.56");
        assertEquals("USD", a.getCurrency());
        assertEquals(0, a.getPrice().compareTo(new BigDecimal("1234.56")));
    }

    @Test
    void extractCurrencyAndNumber_handlesKrDotSpecialCase() {
        Amount a = htmlParser.extractCurrencyAndNumber("kr.99.50");
        // Amount normalises "kr." to "kr"; no further mapping exists, so it passes through.
        assertEquals("kr", a.getCurrency());
        assertEquals(0, a.getPrice().compareTo(new BigDecimal("99.50")));
    }

    @Test
    void extractCurrencyAndNumber_emptyValueProducesZeroAmount() {
        Amount a = htmlParser.extractCurrencyAndNumber("$");
        assertEquals(0, a.getPrice().compareTo(BigDecimal.ZERO));
    }

    @Test
    void extractors_returnEmptyWhenEndMarkerOrStartMarkerAbsent() {
        // endText absent -> endIndex < 0 -> fall through to empty
        assertEquals("", htmlParser.extractText("XSTART_keep", "_", "ZZZ"));
        assertEquals("", htmlParser.extractTextFromStartString("XSTART_keep", "XSTART", "_", "ZZZ"));
        assertEquals("", htmlParser.extractText("XSTART_keep", "XSTART", "_", "ZZZ"));
        assertEquals("", htmlParser.extractRow("before BEGIN payload", "BEGIN", "ZZZ"));

        // single-char startText that is absent -> startIndex == 0 -> empty
        assertEquals("", htmlParser.extractText("abc", "z", "c"));
        assertEquals("", htmlParser.extractTextFromStartString("abc", "a", "z", "c"));
        assertEquals("", htmlParser.extractText("abc", "a", "z", "c"));

        // startString at index 0 -> index not > 0 -> empty
        assertEquals("", htmlParser.extractRow("BEGIN payload", "BEGIN", "ZZZ"));
    }
}

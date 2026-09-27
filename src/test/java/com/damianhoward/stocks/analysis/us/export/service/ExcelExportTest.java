package com.damianhoward.stocks.analysis.us.export.service;

import com.damianhoward.stocks.analysis.us.analysis.domain.AnalysisStock;
import com.damianhoward.stocks.analysis.us.analysis.domain.PegRatios;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.test.util.ReflectionTestUtils;

import java.io.File;
import java.io.UncheckedIOException;
import java.math.BigDecimal;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import static com.damianhoward.stocks.analysis.us.stocklookup.domain.QuoteBuilder.aQuote;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ExcelExportTest {

    @Test
    void generatesWorkbookFromTemplate(@TempDir Path tmp) {
        ExcelExport export = new ExcelExport();
        ReflectionTestUtils.setField(export, "excelTemplate", "template/ZacksPriceDetailsTemplate.xls");
        File out = tmp.resolve("report.xls").toFile();

        export.generateExcel(List.of(), out.getAbsolutePath());

        assertTrue(out.exists(), "excel file should be created from the template");
        assertTrue(out.length() > 0, "excel file should not be empty");
    }

    @Test
    void fillsTemplateRowsWithStockData(@TempDir Path tmp) throws Exception {
        ExcelExport export = new ExcelExport();
        ReflectionTestUtils.setField(export, "excelTemplate", "template/ZacksPriceDetailsTemplate.xls");
        File out = tmp.resolve("report.xls").toFile();

        LocalDate date = LocalDate.of(2024, 6, 1);
        List<AnalysisStock> stocks = List.of(
                row(date, "ACME", "Acme Industries", "42.50", "55.00", "PEG Buy", "Industrials", "Machinery"),
                row(date, "GLBX", "Globex Corp", "210.75", "190.00", "PEG Hold", "Technology", "Software"));

        export.generateExcel(stocks, out.getAbsolutePath());

        // Read the generated workbook back and confirm JXLS bound the rows.
        List<String> values = new ArrayList<>();
        List<Double> numbers = new ArrayList<>();
        try (Workbook wb = WorkbookFactory.create(out)) {
            for (int s = 0; s < wb.getNumberOfSheets(); s++) {
                Sheet sheet = wb.getSheetAt(s);
                for (Row row : sheet) {
                    for (Cell cell : row) {
                        if (cell.getCellType() == org.apache.poi.ss.usermodel.CellType.STRING) {
                            values.add(cell.getStringCellValue());
                        } else if (cell.getCellType() == org.apache.poi.ss.usermodel.CellType.NUMERIC) {
                            numbers.add(cell.getNumericCellValue());
                        }
                    }
                }
            }
        }

        assertTrue(values.contains("Company"), "template header should be present");
        assertTrue(values.contains("Acme Industries"), "first stock's company should be written");
        assertTrue(values.contains("Globex Corp"), "second stock's company should be written");
        assertTrue(values.contains("PEG Buy") && values.contains("PEG Hold"), "per-stock category should be written");
        assertTrue(numbers.containsAll(List.of(42.5, 55.0, 210.75, 190.0)), "quote prices should be written as numbers");
        assertTrue(values.containsAll(List.of("Industrials", "Machinery", "ACME")), "classification should be written");
    }

    @Test
    void aMissingTemplateFailsTheExport(@TempDir Path tmp) {
        ExcelExport export = new ExcelExport();
        ReflectionTestUtils.setField(export, "excelTemplate", "template/does-not-exist.xls");
        File out = tmp.resolve("report.xls").toFile();

        // The email step sends whatever file is there, so a failed export must stop the pipeline.
        assertThrows(UncheckedIOException.class, () -> export.generateExcel(List.of(), out.getAbsolutePath()));

        assertFalse(out.exists(), "no file should be produced when the template is missing");
    }

    private static AnalysisStock row(
            LocalDate date,
            String code,
            String company,
            String price,
            String target,
            String category,
            String sector,
            String industry) {
        return new AnalysisStock(
                null,
                date,
                code,
                null,
                sector,
                null,
                industry,
                aQuote().company(company)
                        .currency("USD")
                        .price(new BigDecimal(price))
                        .targetPrice(new BigDecimal(target))
                        .build(),
                PegRatios.NONE,
                category,
                null);
    }
}

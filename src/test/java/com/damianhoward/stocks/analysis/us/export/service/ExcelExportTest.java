package com.damianhoward.stocks.analysis.us.export.service;

import com.damianhoward.stocks.analysis.us.analysis.domain.AnalysisStock;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.test.util.ReflectionTestUtils;

import java.io.File;
import java.math.BigDecimal;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
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

        List<AnalysisStock> stocks = List.of(
                AnalysisStock.builder()
                        .date(LocalDate.of(2024, 6, 1))
                        .zacksCode("ACME").company("Acme Industries").currency("USD")
                        .price(new BigDecimal("42.50")).targetPrice(new BigDecimal("55.00"))
                        .category("PEG Buy").sectorGroup("Industrials").industry("Machinery").build(),
                AnalysisStock.builder()
                        .date(LocalDate.of(2024, 6, 1))
                        .zacksCode("GLBX").company("Globex Corp").currency("USD")
                        .price(new BigDecimal("210.75")).targetPrice(new BigDecimal("190.00"))
                        .category("PEG Hold").sectorGroup("Technology").industry("Software").build());

        export.generateExcel(stocks, out.getAbsolutePath());

        // Read the generated workbook back and confirm JXLS bound the rows.
        List<String> values = new ArrayList<>();
        try (Workbook wb = WorkbookFactory.create(out)) {
            for (int s = 0; s < wb.getNumberOfSheets(); s++) {
                Sheet sheet = wb.getSheetAt(s);
                for (Row row : sheet) {
                    for (Cell cell : row) {
                        if (cell.getCellType() == org.apache.poi.ss.usermodel.CellType.STRING) {
                            values.add(cell.getStringCellValue());
                        }
                    }
                }
            }
        }

        assertTrue(values.contains("Company"), "template header should be present");
        assertTrue(values.contains("Acme Industries"), "first stock's company should be written");
        assertTrue(values.contains("Globex Corp"), "second stock's company should be written");
        assertTrue(values.contains("PEG Buy") && values.contains("PEG Hold"), "per-stock category should be written");
    }

    @Test
    void missingTemplateIsLoggedNotThrown(@TempDir Path tmp) {
        ExcelExport export = new ExcelExport();
        ReflectionTestUtils.setField(export, "excelTemplate", "template/does-not-exist.xls");
        File out = tmp.resolve("report.xls").toFile();

        // The IOException is caught and logged; the method must not propagate it.
        export.generateExcel(List.of(), out.getAbsolutePath());

        assertFalse(out.exists(), "no file should be produced when the template is missing");
    }
}

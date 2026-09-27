package com.damianhoward.stocks.analysis.us.export.service;

import com.damianhoward.stocks.analysis.us.analysis.domain.AnalysisStock;
import com.damianhoward.stocks.analysis.us.analysis.domain.PegRatios;
import com.damianhoward.stocks.analysis.us.stocklookup.domain.Quote;
import org.jxls.builder.JxlsStreaming;
import org.jxls.transform.poi.JxlsPoi;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UncheckedIOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
public class ExcelExport {

    private static final Logger log = LoggerFactory.getLogger(ExcelExport.class);

    @Value("${stocks.analysis.us.template}")
    private String excelTemplate;

    public void generateExcel(List<AnalysisStock> stocks, String excelFileName) {
        log.info("Generating excel {} using template {}", excelFileName, excelTemplate);
        try (InputStream is = new ClassPathResource(excelTemplate).getInputStream();
                OutputStream os = new FileOutputStream(excelFileName)) {
            Map<String, Object> data = new HashMap<>();
            data.put("stocks", stocks.stream().map(ExcelExport::row).toList());
            log.info("Calling jxls to create excel worksheet {}", excelFileName);
            JxlsPoi.fill(is, JxlsStreaming.STREAMING_OFF, data, os);
            log.info("Completed calling jxls to create excel worksheet {}", excelFileName);
        } catch (IOException e) {
            // The export is the pipeline's output, and the email step sends whatever file is there.
            throw new UncheckedIOException("Could not write " + excelFileName, e);
        }
    }

    /**
     * One report row, keyed by the names the template's cells use ({@code ${stock.NextYearPEG}}).
     * Naming the columns here keeps the template independent of how the domain types are shaped.
     * A {@link HashMap} rather than {@code Map.of}, because an absent figure is a legitimate empty
     * cell.
     */
    private static Map<String, Object> row(AnalysisStock stock) {
        Quote quote = stock.quote();
        PegRatios ratios = stock.ratios();
        Map<String, Object> row = new HashMap<>();
        row.put("Category", stock.category());
        row.put("SectorGroup", stock.sectorGroup());
        row.put("MediumIndustryGroup", stock.mediumIndustryGroup());
        row.put("Industry", stock.industry());
        row.put("ZacksCode", stock.zacksCode());
        row.put("ZacksCompany", stock.zacksCompany());
        row.put("ErrorMessage", stock.errorMessage());
        row.put("Company", quote.company());
        row.put("MarketCap", quote.marketCap());
        row.put("YearEnding", quote.yearEnding());
        row.put("Beta", quote.beta());
        row.put("Price", quote.price());
        row.put("TargetPrice", quote.targetPrice());
        row.put("LastYearEPS", quote.lastYearEPS());
        row.put("LastYearPE", quote.lastYearPE());
        row.put("ThisYearEstimateEPS", quote.thisYearEstimateEPS());
        row.put("NextYearEstimateEPS", quote.nextYearEstimateEPS());
        row.put("EarningAboveEstimates", quote.earningAboveEstimates());
        row.put("RecommendationRating", quote.recommendationRating());
        row.put("ThisYearEstimatePE", ratios.thisYearEstimatePE());
        row.put("NextYearEstimatePE", ratios.nextYearEstimatePE());
        row.put("ThisYearEPSGrowth", ratios.thisYearEPSGrowth());
        row.put("NextYearEPSGrowth", ratios.nextYearEPSGrowth());
        row.put("ThisYearPEG", ratios.thisYearPEG());
        row.put("NextYearPEG", ratios.nextYearPEG());
        return row;
    }
}

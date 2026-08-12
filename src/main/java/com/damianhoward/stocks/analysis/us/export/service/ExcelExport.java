package com.damianhoward.stocks.analysis.us.export.service;

import com.damianhoward.stocks.analysis.us.analysis.domain.AnalysisStock;
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
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
public class ExcelExport {

    private static final Logger log = LoggerFactory.getLogger(ExcelExport.class);

    @Value("${stocks.analysis.us.template}")
    private String excelTemplate;

    public void generateExcel(List<AnalysisStock> stocks, String excelFileName) {
        log.info(String.format("Generating excel %s using template %s", excelFileName, excelTemplate));
        try {
            try (InputStream is = new ClassPathResource(excelTemplate).getInputStream()) {
                try (OutputStream os = new FileOutputStream(excelFileName)) {
                    Map<String, Object> data = new HashMap<>();
                    data.put("stocks", stocks);
                    log.info("Calling jxls to create excel worksheet {}", excelFileName);
                    JxlsPoi.fill(is, JxlsStreaming.STREAMING_OFF, data, os);
                    log.info("Completed calling jxls to create excel worksheet {}", excelFileName);
                }
            }
        } catch (IOException e) {
            log.error("An exception has occurred while attempting to generate excel file", e);
        }
    }

}

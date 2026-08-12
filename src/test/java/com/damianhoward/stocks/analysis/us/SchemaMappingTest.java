package com.damianhoward.stocks.analysis.us;

import com.damianhoward.stocks.analysis.us.stocklookup.domain.StockLookup;
import com.damianhoward.stocks.analysis.us.analysis.domain.AnalysisStock;
import org.junit.jupiter.api.Test;

import jakarta.persistence.Column;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class SchemaMappingTest {

    @Test
    public void targetPriceMappingsUseMigratedColumnName() throws NoSuchFieldException {
        assertEquals("target_price", columnName(StockLookup.class, "targetPrice"));
        assertEquals("target_price", columnName(AnalysisStock.class, "targetPrice"));
    }

    @Test
    public void numericRecommendationRatingMigrationExists() throws IOException {
        String migration = Files.readString(Path.of("src/main/resources/db/migration/V6__align_numeric_columns.sql"));

        assertTrue(migration.contains("ALTER TABLE stock_lookup"));
        assertTrue(migration.contains("ALTER TABLE stock_analysis"));
        assertTrue(migration.contains("recommendation_rating TYPE NUMERIC"));
    }

    private String columnName(Class<?> entityClass, String fieldName) throws NoSuchFieldException {
        return entityClass.getDeclaredField(fieldName).getAnnotation(Column.class).name();
    }
}

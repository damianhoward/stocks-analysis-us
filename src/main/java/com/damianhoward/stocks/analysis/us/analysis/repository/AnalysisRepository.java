package com.damianhoward.stocks.analysis.us.analysis.repository;

import com.damianhoward.stocks.analysis.us.analysis.domain.AnalysisStock;
import org.springframework.data.jdbc.repository.query.Modifying;
import org.springframework.data.jdbc.repository.query.Query;
import org.springframework.data.repository.ListCrudRepository;

import java.time.LocalDate;
import java.util.Set;

public interface AnalysisRepository extends ListCrudRepository<AnalysisStock, Long> {

    @Modifying
    @Query("DELETE FROM stock_analysis WHERE date = :date")
    void deleteByDate(LocalDate date);

    Set<AnalysisStock> findByDate(LocalDate date);
}

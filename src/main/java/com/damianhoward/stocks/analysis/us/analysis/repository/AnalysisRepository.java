package com.damianhoward.stocks.analysis.us.analysis.repository;

import com.damianhoward.stocks.analysis.us.analysis.domain.AnalysisStock;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.Set;

@Component
public interface AnalysisRepository extends JpaRepository<AnalysisStock, Long> {

    @Transactional
    @Modifying
    @Query("delete from AnalysisStock where date =:#{#date}")
    void deleteByDate(@Param("date") LocalDate date);

    Set<AnalysisStock> findByDate(LocalDate date);
}

package com.damianhoward.stocks.analysis.us.stocklookup.repository;

import com.damianhoward.stocks.analysis.us.stocklookup.domain.StockLookup;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.Set;

@Component
public interface StockLookupRepository extends JpaRepository<StockLookup, String> {

    @Transactional
    @Modifying
    @Query("delete from StockLookup where date =:#{#date}")
    void deleteByDate(@Param("date") LocalDate date);

    Set<StockLookup> findByDate(LocalDate date);
}

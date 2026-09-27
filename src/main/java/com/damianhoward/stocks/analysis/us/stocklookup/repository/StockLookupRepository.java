package com.damianhoward.stocks.analysis.us.stocklookup.repository;

import com.damianhoward.stocks.analysis.us.stocklookup.domain.StockLookup;
import org.springframework.data.jdbc.repository.query.Modifying;
import org.springframework.data.jdbc.repository.query.Query;
import org.springframework.data.repository.ListCrudRepository;

import java.time.LocalDate;
import java.util.Set;

public interface StockLookupRepository extends ListCrudRepository<StockLookup, String> {

    @Modifying
    @Query("DELETE FROM stock_lookup WHERE date = :date")
    void deleteByDate(LocalDate date);

    Set<StockLookup> findByDate(LocalDate date);
}

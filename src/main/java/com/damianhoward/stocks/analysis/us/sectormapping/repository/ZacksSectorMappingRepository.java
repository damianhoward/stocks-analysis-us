package com.damianhoward.stocks.analysis.us.sectormapping.repository;

import com.damianhoward.stocks.analysis.us.sectormapping.domain.ZacksSectorMapping;
import org.springframework.data.jdbc.repository.query.Modifying;
import org.springframework.data.jdbc.repository.query.Query;
import org.springframework.data.repository.ListCrudRepository;

import java.time.LocalDate;
import java.util.List;

public interface ZacksSectorMappingRepository extends ListCrudRepository<ZacksSectorMapping, String> {

    @Modifying
    @Query("DELETE FROM zacks_sector_mapping WHERE date = :date")
    void deleteByDate(LocalDate date);

    List<ZacksSectorMapping> findByDate(LocalDate date);
}

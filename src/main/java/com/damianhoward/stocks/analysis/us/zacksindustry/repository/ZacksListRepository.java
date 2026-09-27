package com.damianhoward.stocks.analysis.us.zacksindustry.repository;

import com.damianhoward.stocks.analysis.us.zacksindustry.domain.ZacksList;
import org.springframework.data.jdbc.repository.query.Modifying;
import org.springframework.data.jdbc.repository.query.Query;
import org.springframework.data.repository.ListCrudRepository;

import java.time.LocalDate;
import java.util.Set;

public interface ZacksListRepository extends ListCrudRepository<ZacksList, String> {

    @Modifying
    @Query("DELETE FROM zacks_industry WHERE date = :date")
    void deleteByDate(LocalDate date);

    Set<ZacksList> findByDate(LocalDate date);
}

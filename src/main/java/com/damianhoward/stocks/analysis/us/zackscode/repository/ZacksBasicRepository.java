package com.damianhoward.stocks.analysis.us.zackscode.repository;

import com.damianhoward.stocks.analysis.us.zackscode.domain.ZacksCode;
import org.springframework.data.jdbc.repository.query.Modifying;
import org.springframework.data.jdbc.repository.query.Query;
import org.springframework.data.repository.ListCrudRepository;

import java.time.LocalDate;
import java.util.Set;

public interface ZacksBasicRepository extends ListCrudRepository<ZacksCode, String> {

    @Modifying
    @Query("DELETE FROM zacks_code WHERE date = :date")
    void deleteByDate(LocalDate date);

    Set<ZacksCode> findByDate(LocalDate date);
}

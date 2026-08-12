package com.damianhoward.stocks.analysis.us.zackscode.repository;

import com.damianhoward.stocks.analysis.us.zackscode.domain.ZacksCode;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.Set;

@Component
public interface ZacksBasicRepository extends JpaRepository<ZacksCode, String> {

    @Transactional
    @Modifying
    @Query("delete from ZacksCode where date =:#{#date}")
    void deleteByDate(@Param("date") LocalDate date);

    Set<ZacksCode> findByDate(LocalDate date);
}

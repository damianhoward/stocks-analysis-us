package com.damianhoward.stocks.analysis.us.zacksindustry.repository;

import com.damianhoward.stocks.analysis.us.zacksindustry.domain.ZacksList;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.Set;

@Component
public interface ZacksListRepository extends JpaRepository<ZacksList, String> {

    @Transactional
    @Modifying
    @Query("delete from ZacksList where date =:#{#date}")
    void deleteByDate(@Param("date") LocalDate date);

    Set<ZacksList> findByDate(LocalDate date);
}

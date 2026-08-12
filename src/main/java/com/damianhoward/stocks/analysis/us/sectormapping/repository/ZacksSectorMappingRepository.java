package com.damianhoward.stocks.analysis.us.sectormapping.repository;

import com.damianhoward.stocks.analysis.us.sectormapping.domain.ZacksSectorMapping;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@Component
public interface ZacksSectorMappingRepository extends JpaRepository<ZacksSectorMapping, String> {

    @Transactional
    @Modifying
    @Query("delete from ZacksSectorMapping where date =:#{#date}")
    void deleteByDate(@Param("date") LocalDate date);

    List<ZacksSectorMapping> findByDate(LocalDate date);
}

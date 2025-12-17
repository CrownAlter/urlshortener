package com.adewunmi.urlshortener.repository;

import com.adewunmi.urlshortener.entity.Click;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface ClickRepository extends JpaRepository<Click, Long> {
    long countByUrlId(Long urlId);

    List<Click> findTop10ByUrlIdOrderByClickedAtDesc(Long urlId);

    @Query("SELECT DATE(c.clickedAt) as date, COUNT(c) as clicks " +
            "FROM Click c WHERE c.url.id = :urlId " +
            "GROUP BY DATE(c.clickedAt) " +
            "ORDER BY DATE(c.clickedAt) DESC")
    List<Object[]> findClicksByDate(@Param("urlId") Long urlId);

    @Query("SELECT COALESCE(c.referrer, 'Direct') as referrer, COUNT(c) as clicks " +
            "FROM Click c WHERE c.url.id = :urlId " +
            "GROUP BY c.referrer " +
            "ORDER BY COUNT(c) DESC")
    List<Object[]> findTopReferrers(@Param("urlId") Long urlId);

    List<Click> findByUrlIdAndClickedAtAfter(Long urlId, LocalDateTime after);
}
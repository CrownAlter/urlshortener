package com.adewunmi.urlshortener.repository;

import com.adewunmi.urlshortener.entity.Url;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UrlRepository extends JpaRepository<Url, Long> {

    Optional<Url> findByShortCode(String shortCode);

    boolean existsByShortCode(String shortCode);

    @Query("SELECT u FROM Url u LEFT JOIN FETCH u.clicks ORDER BY u.createdAt DESC")
    List<Url> findAllWithClicks();

    Optional<Url> findByOriginalUrl(String originalUrl);
}
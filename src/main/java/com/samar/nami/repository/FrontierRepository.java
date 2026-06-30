package com.samar.nami.repository;

import com.samar.nami.entity.Frontier;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

public interface FrontierRepository extends JpaRepository<Frontier, Long> {

    // get the next URL waiting to be crawled
    Optional<Frontier> findFirstByStatus(String status);

    // how many are still pending (for the loop condition)
    long countByStatus(String status);

    /**
     * Insert a discovered URL as PENDING, letting the DB handle dedup.
     * The unique constraint on "url" rejects duplicates, and ON CONFLICT DO NOTHING
     * turns that rejection into a silent skip instead of an error. This replaces the
     * old in-memory "known" HashSet: Postgres is now the single source of truth for
     * which URLs have been seen, so we no longer mirror ~10M URLs in RAM.
     */
    @Modifying
    @Transactional
    @Query(value = """
        INSERT INTO frontier (url, status)
        VALUES (:url, :status)
        ON CONFLICT (url) DO NOTHING
        """, nativeQuery = true)
    void insertIfNew(@Param("url") String url, @Param("status") String status);
}

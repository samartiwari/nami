package com.samar.nami.repository;

import com.samar.nami.entity.Frontier;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface FrontierRepository extends JpaRepository<Frontier, Long> {

    // get the next URL waiting to be crawled
    Optional<Frontier> findFirstByStatus(String status);

    // load all known URLs at startup (for the in-memory dedup cache)
    @org.springframework.data.jpa.repository.Query("select f.url from Frontier f")
    List<String> findAllUrls();

    // how many are still pending (for the loop condition)
    long countByStatus(String status);
}

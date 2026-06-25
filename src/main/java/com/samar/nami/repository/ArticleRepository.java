package com.samar.nami.repository;

import com.samar.nami.entity.Article;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface ArticleRepository extends JpaRepository<Article, Long> {

    boolean existsByUrl(String url);

    @Query("SELECT AVG(a.totalWords) FROM Article a")
    Double findAverageTotalWords();
}

package com.samar.nami.repository;

import com.samar.nami.entity.Article;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ArticleRepository extends JpaRepository<Article, Long> {

    boolean existsByUrl(String url);
}

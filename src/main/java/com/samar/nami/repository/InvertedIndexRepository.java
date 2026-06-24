package com.samar.nami.repository;

import com.samar.nami.entity.InvertedIndex;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface InvertedIndexRepository extends JpaRepository<InvertedIndex, Long> {

    // search for a single word → get back all article IDs containing it
    @Query("SELECT i.articleId FROM InvertedIndex i WHERE i.word = :word")
    List<Long> findArticleIdsByWord(String word);
}

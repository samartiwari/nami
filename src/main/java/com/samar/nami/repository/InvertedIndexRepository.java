package com.samar.nami.repository;

import com.samar.nami.entity.InvertedIndex;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface InvertedIndexRepository extends JpaRepository<InvertedIndex, Long> {

    // returns full entities (we need articleId + count for TF-IDF scoring)
    List<InvertedIndex> findByWord(String word);
}

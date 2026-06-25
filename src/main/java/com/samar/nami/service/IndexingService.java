package com.samar.nami.service;

import com.samar.nami.entity.InvertedIndex;
import com.samar.nami.repository.InvertedIndexRepository;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class IndexingService {

    private final InvertedIndexRepository invertedIndexRepository;
    private final TextProcessor textProcessor;

    public IndexingService(InvertedIndexRepository invertedIndexRepository, TextProcessor textProcessor) {
        this.invertedIndexRepository = invertedIndexRepository;
        this.textProcessor = textProcessor;
    }

    /**
     * Tokenize the full page text, count word frequencies, and store in the inverted index.
     */
    public void indexPage(Long articleId, String fullText) {
        // 1. tokenize (duplicates preserved — we need them for counting)
        List<String> tokens = textProcessor.tokenize(fullText);

        // 2. count occurrences of each word
        Map<String, Integer> wordCounts = new HashMap<>();
        for (String token : tokens) {
            wordCounts.merge(token, 1, Integer::sum);
        }

        // 3. batch insert all (word, articleId, count) rows
        List<InvertedIndex> rows = new ArrayList<>();
        for (Map.Entry<String, Integer> entry : wordCounts.entrySet()) {
            rows.add(new InvertedIndex(entry.getKey(), articleId, entry.getValue()));
        }
        invertedIndexRepository.saveAll(rows);
    }
}

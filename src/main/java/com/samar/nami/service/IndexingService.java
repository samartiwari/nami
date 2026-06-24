package com.samar.nami.service;

import com.samar.nami.entity.InvertedIndex;
import com.samar.nami.repository.InvertedIndexRepository;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
public class IndexingService {

    private final InvertedIndexRepository invertedIndexRepository;
    private final Set<String> stopWords;

    public IndexingService(InvertedIndexRepository invertedIndexRepository) throws IOException {
        this.invertedIndexRepository = invertedIndexRepository;
        this.stopWords = loadStopWords();
    }

    /**
     * Load stop words from stopwords.txt in resources at startup (once).
     */
    private Set<String> loadStopWords() throws IOException {
        Set<String> words = new HashSet<>();
        BufferedReader reader = new BufferedReader(
            new InputStreamReader(new ClassPathResource("stopwords.txt").getInputStream())
        );
        String line;
        while ((line = reader.readLine()) != null) {
            line = line.trim().toLowerCase();
            if (!line.isEmpty()) {
                words.add(line);
            }
        }
        reader.close();
        System.out.println("Loaded " + words.size() + " stop words");
        return words;
    }

    /**
     * Tokenize the full page text and store each unique word → articleId mapping.
     */
    public void indexPage(Long articleId, String fullText) {
        // 1. split on any non-word character (spaces, punctuation, brackets, etc.)
        String[] tokens = fullText.toLowerCase().split("\\W+");

        // 2. deduplicate + filter: only keep meaningful words
        Set<String> uniqueWords = new HashSet<>();
        for (String token : tokens) {
            if (token.length() > 2 && !stopWords.contains(token)) {
                uniqueWords.add(token);
            }
        }

        // 3. batch insert all (word, articleId) rows
        List<InvertedIndex> rows = new ArrayList<>();
        for (String word : uniqueWords) {
            rows.add(new InvertedIndex(word, articleId));
        }
        invertedIndexRepository.saveAll(rows);
    }
}

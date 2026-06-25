package com.samar.nami.service;

import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Shared text processing: stop words + tokenization.
 * Used by both IndexingService (during crawl) and SearchService (during search).
 * Same logic in both places = same words get indexed and searched.
 */
@Component
public class TextProcessor {

    private final Set<String> stopWords;

    public TextProcessor() throws IOException {
        this.stopWords = loadStopWords();
    }

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
     * Tokenize text into a list of meaningful words (with duplicates preserved for counting).
     */
    public List<String> tokenize(String text) {
        String[] tokens = text.toLowerCase().split("\\W+");
        List<String> result = new ArrayList<>();
        for (String token : tokens) {
            if (token.length() > 2 && !stopWords.contains(token)) {
                result.add(token);
            }
        }
        return result;
    }
}

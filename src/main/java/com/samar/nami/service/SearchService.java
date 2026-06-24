package com.samar.nami.service;

import com.samar.nami.entity.Article;
import com.samar.nami.repository.ArticleRepository;
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
public class SearchService {

    private final InvertedIndexRepository invertedIndexRepository;
    private final ArticleRepository articleRepository;
    private final Set<String> stopWords;

    public SearchService(InvertedIndexRepository invertedIndexRepository,
                         ArticleRepository articleRepository) throws IOException {
        this.invertedIndexRepository = invertedIndexRepository;
        this.articleRepository = articleRepository;
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
        return words;
    }

    /**
     * Search for articles matching the query.
     * Tokenizes the query the same way as indexing, then finds articles containing ALL words.
     */
    public List<Article> search(String query) {
        // tokenize query the same way we tokenize pages
        String[] tokens = query.toLowerCase().split("\\W+");

        List<String> queryWords = new ArrayList<>();
        for (String token : tokens) {
            if (token.length() > 2 && !stopWords.contains(token)) {
                queryWords.add(token);
            }
        }

        if (queryWords.isEmpty()) {
            return List.of();
        }

        // start with article IDs matching the first word
        Set<Long> resultIds = new HashSet<>(invertedIndexRepository.findArticleIdsByWord(queryWords.get(0)));

        // intersect with article IDs for each additional word
        // → only articles containing ALL query words survive
        for (int i = 1; i < queryWords.size(); i++) {
            Set<Long> nextIds = new HashSet<>(invertedIndexRepository.findArticleIdsByWord(queryWords.get(i)));
            resultIds.retainAll(nextIds);

            if (resultIds.isEmpty()) {
                return List.of();   // no article has all words, stop early
            }
        }

        return articleRepository.findAllById(resultIds);
    }
}

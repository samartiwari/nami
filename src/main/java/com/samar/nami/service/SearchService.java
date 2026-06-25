package com.samar.nami.service;

import com.samar.nami.dto.SearchResult;
import com.samar.nami.entity.Article;
import com.samar.nami.entity.InvertedIndex;
import com.samar.nami.repository.ArticleRepository;
import com.samar.nami.repository.InvertedIndexRepository;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class SearchService {

    // BM25 tuning parameters (standard defaults used by Elasticsearch)
    private static final double K1 = 1.2;  // TF saturation speed
    private static final double B = 0.75;  // document length normalization strength

    private final InvertedIndexRepository invertedIndexRepository;
    private final ArticleRepository articleRepository;
    private final TextProcessor textProcessor;

    public SearchService(InvertedIndexRepository invertedIndexRepository,
                         ArticleRepository articleRepository,
                         TextProcessor textProcessor) {
        this.invertedIndexRepository = invertedIndexRepository;
        this.articleRepository = articleRepository;
        this.textProcessor = textProcessor;
    }

    /**
     * Search using BM25 ranking with OR semantics.
     * Articles matching ANY query word are returned, sorted by relevance score.
     */
    public List<SearchResult> search(String query) {
        List<String> queryWords = textProcessor.tokenize(query);

        if (queryWords.isEmpty()) {
            return List.of();
        }

        long totalArticles = articleRepository.count();
        if (totalArticles == 0) {
            return List.of();
        }

        // average document length across all articles (for BM25 length normalization)
        Double avgDl = articleRepository.findAverageTotalWords();
        if (avgDl == null || avgDl == 0) {
            return List.of();
        }

        // we need each article's totalWords for BM25, so pre-fetch all articles
        // and build a lookup: articleId → totalWords
        Map<Long, Integer> docLengths = new HashMap<>();
        for (Article article : articleRepository.findAll()) {
            docLengths.put(article.getId(), article.getTotalWords());
        }

        // accumulate BM25 score per article across all query words
        Map<Long, Double> scores = new HashMap<>();

        for (String word : queryWords) {
            List<InvertedIndex> matches = invertedIndexRepository.findByWord(word);

            int df = matches.size();
            if (df == 0) continue;

            // BM25 IDF (Lucene's version): log(1 + (N - df + 0.5) / (df + 0.5))
            // Adding 1 prevents negative IDF when a word appears in >50% of documents
            double idf = Math.log(1.0 + ((double) totalArticles - df + 0.5) / (df + 0.5));

            for (InvertedIndex match : matches) {
                double tf = match.getCount();
                int docLength = docLengths.getOrDefault(match.getArticleId(), 1);

                // BM25 score for this (word, article) pair:
                // IDF × (tf × (k1 + 1)) / (tf + k1 × (1 - b + b × (docLength / avgDl)))
                double numerator = tf * (K1 + 1);
                double denominator = tf + K1 * (1 - B + B * (docLength / avgDl));
                double bm25 = idf * (numerator / denominator);

                scores.merge(match.getArticleId(), bm25, Double::sum);
            }
        }

        if (scores.isEmpty()) {
            return List.of();
        }

        // sort article IDs by score (highest first)
        List<Long> rankedIds = new ArrayList<>(scores.keySet());
        rankedIds.sort((a, b) -> Double.compare(scores.get(b), scores.get(a)));

        // fetch articles and pair with scores
        Map<Long, Article> articleMap = new HashMap<>();
        for (Article article : articleRepository.findAllById(rankedIds)) {
            articleMap.put(article.getId(), article);
        }

        List<SearchResult> results = new ArrayList<>();
        for (Long id : rankedIds) {
            Article article = articleMap.get(id);
            if (article != null) {
                results.add(new SearchResult(article, scores.get(id)));
            }
        }

        return results;
    }
}

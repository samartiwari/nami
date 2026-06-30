package com.samar.nami.service;

import com.samar.nami.dto.SearchResponse;
import com.samar.nami.dto.SearchResult;
import com.samar.nami.entity.Article;
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

    // Fuzzy "did you mean?" tuning: ignore correction candidates that appear in
    // fewer than this many articles (df floor), so suggestions are real, central
    // words rather than rare junk. Measured against the current crawl (50 articles,
    // ~18.5k distinct words, of which ~55% appear in only 1 article): a floor of 5
    // means "in >=10% of articles", a strong signal. Raise as the crawl grows.
    private static final int MIN_DF = 5;

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

    // Pagination defaults / guard rails.
    private static final int DEFAULT_PAGE = 0;
    private static final int DEFAULT_SIZE = 10;
    private static final int MAX_SIZE = 50;  // cap so size=99999 can't defeat pagination

    /**
     * Small holder for the FULL ranked result list plus the optional suggestion,
     * before pagination is applied. Lets the orchestration logic stay clean and
     * keeps slicing in exactly one place.
     */
    /** One page of ranked results: the page's rows, the optional suggestion, and the full match count. */
    private record Ranked(List<SearchResult> results, String didYouMean, long totalResults) {}

    /**
     * Public entry point. Runs the search and returns a single page of results.
     *
     * Ranking + pagination now happen in Postgres (BM25 as a SQL query with
     * LIMIT/OFFSET) — Java only ever receives the requested page, never the full
     * match set. So page N is fetched directly, not sliced from an in-memory list.
     */
    public SearchResponse search(String query, Integer page, Integer size) {
        int p = (page == null || page < 0) ? DEFAULT_PAGE : page;
        int s = (size == null || size < 1) ? DEFAULT_SIZE : Math.min(size, MAX_SIZE);

        Ranked ranked = runRanked(query, p, s);
        int totalPages = (int) Math.ceil((double) ranked.totalResults() / s);

        return new SearchResponse(ranked.results(), ranked.didYouMean(),
                                  p, s, ranked.totalResults(), totalPages);
    }

    /**
     * Fetches ONE page of ranked results plus the optional "Did you mean?" suggestion.
     *
     * First runs a normal BM25 search. If it returns results, we are done.
     * If it returns nothing, we try the fuzzy fallback: correct each query word
     * to its nearest indexed word (trigram similarity), re-run BM25, and surface
     * the correction as a suggestion.
     */
    private Ranked runRanked(String query, int page, int size) {
        List<String> queryWords = textProcessor.tokenize(query);

        if (queryWords.isEmpty()) {
            return new Ranked(List.of(), null, 0);
        }

        // 1. Normal search — the happy path. Fuzzy logic never runs if this hits.
        long total = invertedIndexRepository.countMatching(queryWords);
        if (total > 0) {
            List<SearchResult> results = runBm25(queryWords, page, size);
            return new Ranked(results, null, total);
        }

        // 2. Zero results — try correcting each word to its nearest indexed word.
        List<String> correctedWords = new ArrayList<>();
        boolean changed = false;
        for (String word : queryWords) {
            String nearest = invertedIndexRepository.findNearestWord(word, MIN_DF);
            if (nearest != null && !nearest.equals(word)) {
                correctedWords.add(nearest);
                changed = true;
            } else {
                correctedWords.add(word);  // no good candidate — keep original
            }
        }

        // Nothing got corrected → no honest suggestion to make.
        if (!changed) {
            return new Ranked(List.of(), null, 0);
        }

        // 3. Re-run BM25 with the corrected query and surface the suggestion.
        long correctedTotal = invertedIndexRepository.countMatching(correctedWords);
        List<SearchResult> correctedResults = runBm25(correctedWords, page, size);
        String didYouMean = String.join(" ", correctedWords);
        return new Ranked(correctedResults, didYouMean, correctedTotal);
    }

    /**
     * BM25 ranking for ONE page, computed entirely in Postgres.
     *
     * The DB scores every matching (word, article) posting, sums per article,
     * sorts, and returns only this page (LIMIT/OFFSET). Java then fetches the
     * page's articles by id to build the result DTOs. We never pull the full
     * match set into Java — that was the old O(matches) bottleneck.
     */
    private List<SearchResult> runBm25(List<String> queryWords, int page, int size) {
        long totalArticles = articleRepository.count();
        Double avgDl = articleRepository.findAverageTotalWords();
        if (totalArticles == 0 || avgDl == null || avgDl == 0) {
            return List.of();
        }

        int offset = page * size;

        // Postgres ranks all matches and ships back only this page's (id, score).
        List<InvertedIndexRepository.ScoredArticle> ranked =
                invertedIndexRepository.searchBm25(queryWords, totalArticles, avgDl, K1, B, size, offset);

        if (ranked.isEmpty()) {
            return List.of();
        }

        // preserve the score order while fetching the page's articles by id
        List<Long> pageIds = new ArrayList<>();
        Map<Long, Double> scoreById = new HashMap<>();
        for (InvertedIndexRepository.ScoredArticle row : ranked) {
            pageIds.add(row.getArticleId());
            scoreById.put(row.getArticleId(), row.getScore());
        }

        Map<Long, Article> articleMap = new HashMap<>();
        for (Article article : articleRepository.findAllById(pageIds)) {
            articleMap.put(article.getId(), article);
        }

        List<SearchResult> results = new ArrayList<>();
        for (Long id : pageIds) {
            Article article = articleMap.get(id);
            if (article != null) {
                results.add(new SearchResult(article, scoreById.get(id)));
            }
        }
        return results;
    }
}

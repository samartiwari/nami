package com.samar.nami.service;

import com.samar.nami.entity.Article;
import com.samar.nami.repository.ArticleRepository;
import com.samar.nami.repository.InvertedIndexRepository;
import org.springframework.stereotype.Service;

/**
 * Shared "store + index one article" logic.
 *
 * Both the crawler (live HTML pages) and the /ingest endpoint (Wikipedia dump
 * articles) call this, so an article is saved and indexed the SAME way no matter
 * where it came from. This is the consistency guarantee: identical totalWords
 * calculation and identical tokenization/indexing path → indexed words always
 * match searched words.
 */
@Service
public class IngestService {

    private static final int SNIPPET_MAX = 200;

    private final ArticleRepository articleRepository;
    private final IndexingService indexingService;
    private final InvertedIndexRepository invertedIndexRepository;

    public IngestService(ArticleRepository articleRepository, IndexingService indexingService,
                         InvertedIndexRepository invertedIndexRepository) {
        this.articleRepository = articleRepository;
        this.indexingService = indexingService;
        this.invertedIndexRepository = invertedIndexRepository;
    }

    /**
     * Rebuild the precomputed word_stats(word, df) table from inverted_index.
     * Call this after a crawl/load batch so fuzzy "did you mean" reads fresh,
     * fast df values instead of counting postings live. One bulk query (~seconds).
     */
    public void rebuildWordStats() {
        invertedIndexRepository.rebuildWordStats();
    }

    /**
     * Store one article and index its full text.
     *
     * @param title    article title
     * @param url      unique URL (used for dedup)
     * @param snippet  short preview; if null, one is derived from fullText
     * @param fullText the text to index
     * @return true if stored+indexed, false if it already existed (skipped)
     */
    public boolean ingest(String title, String url, String snippet, String fullText) {
        // skip if we already have this article (same dedup rule as the crawler)
        if (articleRepository.existsByUrl(url)) {
            return false;
        }

        // totalWords computed exactly as the crawler does it (BM25 length normalization)
        int totalWords = fullText.split("\\W+").length;

        // derive a snippet from the text if the caller didn't supply one
        if (snippet == null || snippet.isBlank()) {
            snippet = deriveSnippet(fullText);
        }

        // save first (need the generated ID), then index the full text
        Article saved = articleRepository.save(new Article(title, url, snippet, totalWords));
        indexingService.indexPage(saved.getId(), fullText);
        return true;
    }

    private String deriveSnippet(String fullText) {
        String trimmed = fullText.strip();
        if (trimmed.length() <= SNIPPET_MAX) {
            return trimmed;
        }
        return trimmed.substring(0, SNIPPET_MAX) + "...";
    }
}

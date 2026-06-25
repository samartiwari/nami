package com.samar.nami.dto;

import com.samar.nami.entity.Article;
import lombok.Getter;

/**
 * Wraps an Article with its TF-IDF relevance score for search results.
 */
@Getter
public class SearchResult {

    private final Long id;
    private final String title;
    private final String url;
    private final String snippet;
    private final double score;

    public SearchResult(Article article, double score) {
        this.id = article.getId();
        this.title = article.getTitle();
        this.url = article.getUrl();
        this.snippet = article.getSnippet();
        this.score = Math.round(score * 100.0) / 100.0;  // round to 2 decimals
    }
}

package com.samar.nami.dto;

import lombok.Getter;

import java.util.List;

/**
 * Top-level search response.
 * Carries one page of ranked results, an optional "Did you mean?" suggestion,
 * and pagination metadata so a UI can render page controls.
 *
 * didYouMean is null on the happy path (normal search returned results);
 * it is set only when the fuzzy fallback corrected the query.
 */
@Getter
public class SearchResponse {

    private final List<SearchResult> results;  // the current page's slice
    private final String didYouMean;
    private final int page;          // zero-based page index echoed back
    private final int size;          // page size used
    private final long totalResults; // full count before slicing
    private final int totalPages;    // ceil(totalResults / size)

    public SearchResponse(List<SearchResult> results, String didYouMean,
                          int page, int size, long totalResults, int totalPages) {
        this.results = results;
        this.didYouMean = didYouMean;
        this.page = page;
        this.size = size;
        this.totalResults = totalResults;
        this.totalPages = totalPages;
    }
}

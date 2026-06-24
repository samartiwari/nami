package com.samar.nami.controller;

import com.samar.nami.entity.Article;
import com.samar.nami.service.SearchService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
public class SearchController {

    private final SearchService searchService;

    public SearchController(SearchService searchService) {
        this.searchService = searchService;
    }

    @GetMapping("/search")
    public List<Article> search(@RequestParam String q) {
        return searchService.search(q);
    }
}

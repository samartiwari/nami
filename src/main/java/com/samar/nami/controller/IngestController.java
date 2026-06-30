package com.samar.nami.controller;

import com.samar.nami.dto.IngestRequest;
import com.samar.nami.service.IngestService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

/**
 * Thin HTTP layer for bulk-loading articles (e.g. from a Wikipedia dump).
 * Delegates entirely to IngestService so dump articles are stored + indexed
 * the SAME way crawled pages are. Returns only a status code (cheapest):
 *   200 OK       -> stored + indexed
 *   409 Conflict -> already existed, skipped
 */
@RestController
public class IngestController {

    private final IngestService ingestService;

    public IngestController(IngestService ingestService) {
        this.ingestService = ingestService;
    }

    @PostMapping("/ingest")
    public ResponseEntity<Void> ingest(@RequestBody IngestRequest req) {
        boolean stored = ingestService.ingest(req.title(), req.url(), null, req.text());
        return stored
                ? ResponseEntity.ok().build()
                : ResponseEntity.status(HttpStatus.CONFLICT).build();
    }
}

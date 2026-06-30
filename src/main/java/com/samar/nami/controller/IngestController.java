package com.samar.nami.controller;

/**
 * HTTP endpoints for bulk-loading articles (POST /ingest) and refreshing the
 * df table (POST /rebuild-word-stats) — used by the Python dump loader.
 *
 * DISABLED for the deployed, crawler-driven search engine: the public app should
 * expose only /search, not loading/admin endpoints. The underlying IngestService
 * methods are kept and still used internally by the crawler (which auto-rebuilds
 * word_stats at the end of a run). To re-enable dump loading, uncomment below and
 * restore the imports.
 */
// @org.springframework.web.bind.annotation.RestController
public class IngestController {

    // private final com.samar.nami.service.IngestService ingestService;
    //
    // public IngestController(com.samar.nami.service.IngestService ingestService) {
    //     this.ingestService = ingestService;
    // }
    //
    // @org.springframework.web.bind.annotation.PostMapping("/ingest")
    // public org.springframework.http.ResponseEntity<Void> ingest(
    //         @org.springframework.web.bind.annotation.RequestBody com.samar.nami.dto.IngestRequest req) {
    //     boolean stored = ingestService.ingest(req.title(), req.url(), null, req.text());
    //     return stored
    //             ? org.springframework.http.ResponseEntity.ok().build()
    //             : org.springframework.http.ResponseEntity.status(
    //                   org.springframework.http.HttpStatus.CONFLICT).build();
    // }
    //
    // @org.springframework.web.bind.annotation.PostMapping("/rebuild-word-stats")
    // public org.springframework.http.ResponseEntity<Void> rebuildWordStats() {
    //     ingestService.rebuildWordStats();
    //     return org.springframework.http.ResponseEntity.ok().build();
    // }
}

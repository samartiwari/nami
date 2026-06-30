package com.samar.nami.dto;

/**
 * Incoming body for POST /ingest — one article from the loader.
 * snippet is omitted on purpose; IngestService derives it from the text.
 */
public record IngestRequest(String title, String url, String text) {}

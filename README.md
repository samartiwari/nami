# Nami — a Wikipedia search engine

Nami is a **from-scratch full-text search engine** over Wikipedia. It crawls articles,
builds its own inverted index, and ranks results with **BM25** — the same relevance
algorithm Elasticsearch uses — but implemented directly on PostgreSQL, **without any
off-the-shelf search engine**. The goal was to understand and build the internals of a
search engine end to end: crawling, indexing, ranking, fuzzy matching, and the query
optimization that keeps it fast at scale.

## What it does

- **Crawls** Wikipedia from a seed URL, following links, and indexes each page.
- **Searches** that index with BM25 relevance ranking, returning the best matches.
- **Suggests corrections** ("did you mean?") for typos using trigram similarity.
- **Scales**: benchmarked at **~143,000 articles** with **sub-second search on every
  query**, including the worst-case word that appears in ~90% of articles.

## What was achieved

- Implemented **BM25 ranking from scratch** (no Elasticsearch/Lucene index) — including
  document-length normalization and a Lucene-safe IDF formula.
- **Optimized search latency ~6×** (a common-word query went from **1.2s to 0.19s**) by
  moving ranking computation into SQL and adding a covering index.
- **Optimized fuzzy "did you mean" ~47×** (from **~9s to ~0.15s**) by precomputing
  document frequencies instead of counting them live on every query.
- Built a **resumable crawler** with database-backed deduplication, so a multi-day crawl
  survives restarts and never re-visits a URL — without holding the URL set in memory.

See the [Performance](#performance) section for the full optimization breakdown.

## Features

- **Crawler** — follows Wikipedia's link graph from a seed URL, resumable across
  restarts (the frontier and crawled data are persisted, so it picks up where it
  left off).
- **Inverted index** — `(word, articleId, count)` rows, with stop-word removal and
  Snowball (Porter) stemming applied identically at index time and query time.
- **BM25 ranking** — the industry-standard relevance algorithm, computed entirely in
  PostgreSQL (scoring, sorting, and pagination happen in SQL; the app never pulls the
  full match set into memory).
- **Fuzzy search ("did you mean?")** — when a query returns nothing, the nearest
  indexed word is found via PostgreSQL trigram similarity (`pg_trgm`) and the search
  is re-run on the correction.
- **Pagination** — `page` / `size` parameters, sliced in SQL via `LIMIT`/`OFFSET`.

## Performance

Measured at ~143k articles (Wikipedia subset). Three optimizations took common-word
search from over a second to well under it:

| Optimization | What it did | Result |
|---|---|---|
| BM25 ranking moved into SQL | Stop transferring/looping over all matches in Java | `history`: 1.2s → 0.19s |
| Composite covering index `(word, article_id, count)` | Index-only scans, no table lookups | `reference` (in ~90% of articles): 1.1s → 0.5s |
| Precomputed `word_stats` (document frequency) table | Fuzzy lookup reads df instead of counting live | typo search: ~9s → ~0.15s |

Rare-word searches run in ~20–30ms; the most common word in the corpus stays under a
second.

## Tech stack

- Java 17, Spring Boot (Spring Data JPA, Spring Web MVC)
- PostgreSQL 16 with the `pg_trgm` extension
- Jsoup (HTML parsing during crawl)
- Lucene `analysis-common` (Snowball/Porter stemming)
- Python (`datasets`, `pyarrow`, `requests`) for the optional bulk loader

## Getting started

### 1. Prerequisites

- Java 17+ and Maven (the project ships with the Maven wrapper `./mvnw`)
- PostgreSQL 16 with the `pg_trgm` extension available
- (Optional) Python 3.12+ if you want to bulk-load from a Wikipedia dump

### 2. Database

Create a database and a user, then point the app at it with these environment
variables (read by `application.properties`):

```bash
export DB_HOST=localhost
export DB_PORT=5432
export DB_NAME=nami
export DB_USER=nami
export DB_PASSWORD=yourpassword
```

The `pg_trgm` extension and all indexes (including the trigram and BM25 indexes) are
created automatically on startup by `src/main/resources/schema.sql`.

### 3. Run

```bash
./mvnw spring-boot:run
```

On startup the app begins crawling from the seed URL and indexing pages. Crawling is
controlled by `nami.crawler.enabled` in `application.properties`:

- `true` — auto-crawl on boot (resumes from the persisted frontier)
- `false` — don't crawl (e.g. when you just want to serve search over an existing index)

### 4. Search

```
GET /search?q=<query>&page=<0-based>&size=<n>
```

Example:

```bash
curl "http://localhost:8080/search?q=albert+einstein&size=10"
```

Response:

```json
{
  "results": [
    { "id": 42, "title": "Albert Einstein", "url": "...", "snippet": "...", "score": 8.31 }
  ],
  "didYouMean": null,
  "page": 0,
  "size": 10,
  "totalResults": 1234,
  "totalPages": 124
}
```

`didYouMean` is `null` on a normal search; it is set to a corrected query when the
original returned nothing and a close match was found.

## Optional: bulk-loading from a Wikipedia dump (`loader.py`)

Crawling a large corpus takes days. For faster bulk-loading during development, the
repo includes `loader.py`, which downloads one parquet file from the Hugging Face
`wikimedia/wikipedia` dataset and feeds its articles into the app.

**Why this needs a Python virtual environment:** `loader.py` depends on the
`datasets`, `pyarrow`, and `requests` libraries. The standard, isolated way to install
Python dependencies (without touching system Python — which modern Linux blocks
anyway) is a venv:

```bash
# create an isolated environment for this project's Python deps
python3 -m venv .venv

# install the loader's dependencies into it
.venv/bin/pip install datasets requests

# run the loader (the Spring app must be running first)
.venv/bin/python loader.py
```

The `.venv/` directory is git-ignored — it is regenerated from the commands above and
should never be committed.

> **Note:** the bulk-loader requires the ingestion endpoints described below to be
> enabled.

## Why the ingestion endpoints are disabled

`IngestController` defines two endpoints that are **commented out by default**:

- `POST /ingest` — store + index a single article (used by `loader.py`)
- `POST /rebuild-word-stats` — manually rebuild the precomputed document-frequency table

These exist only for **bulk-loading / administration**, not for a public search
deployment. A deployed, crawler-driven search engine should expose **only `/search`**,
so these endpoints are disabled to keep the public surface read-only.

They are kept (commented out, with their underlying `IngestService` methods intact)
rather than deleted, because:

1. The bulk loader (`loader.py`) needs `/ingest` — re-enable it (uncomment the methods
   and restore the `@RestController` annotation in `IngestController`) when loading a
   dump.
2. The crawler still calls the underlying indexing/rebuild logic internally, so the
   service code must remain.

The `word_stats` (document-frequency) table is otherwise kept fresh automatically: it
is rebuilt on a schedule (hourly) while the app runs and once more at the end of each
crawl, so fuzzy search stays accurate without any manual endpoint call.

## Project layout

```
src/main/java/com/samar/nami/
├── bootstrap/CrawlerStarter.java     # kicks off the crawl on startup (if enabled)
├── controller/
│   ├── SearchController.java         # GET /search  (the public API)
│   └── IngestController.java         # POST /ingest, /rebuild-word-stats (disabled)
├── service/
│   ├── CrawlerService.java           # crawl loop, link following, dedup
│   ├── IndexingService.java          # tokenize + store inverted-index rows
│   ├── IngestService.java            # shared "store + index one article" logic
│   ├── SearchService.java            # BM25 search orchestration + fuzzy fallback
│   ├── TextProcessor.java            # stop words + stemming (shared index/query)
│   └── WordStatsScheduler.java       # periodic df-table rebuild
├── repository/                       # JPA + native SQL (BM25, trigram, df rebuild)
└── entity/                           # Article, InvertedIndex, Frontier

src/main/resources/
├── schema.sql                        # pg_trgm + all indexes, created on startup
├── stopwords.txt                     # English stop words + Wikipedia boilerplate
└── application.properties

loader.py                             # optional Hugging Face parquet bulk-loader
```

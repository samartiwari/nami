-- pg_trgm powers fuzzy "Did you mean?" search (trigram similarity on indexed words).
-- Re-created on every boot because ddl-auto=create-drop rebuilds the schema each time.
CREATE EXTENSION IF NOT EXISTS pg_trgm;

-- GIN trigram index on the word column. Turns findNearestWord's "WHERE word % :input"
-- from a full scan of every distinct word into a jump to the few words sharing trigrams.
-- Built after Hibernate creates the table (defer-datasource-initialization=true).
CREATE INDEX IF NOT EXISTS idx_inverted_index_word_trgm
    ON inverted_index USING gin (word gin_trgm_ops);

-- Composite COVERING index for the BM25 search query. Because it stores all three
-- columns the query needs (word, article_id, count), Postgres answers the posting
-- lookup with an "Index Only Scan" — it reads everything from the index and never
-- touches the table. This eliminated the lossy bitmap recheck (re-reading ~1.46M
-- rows) that made common words slow: e.g. "reference" (in ~91% of articles) dropped
-- from ~1.1s to ~0.5s at 100k articles.
CREATE INDEX IF NOT EXISTS idx_word_article_count
    ON inverted_index (word, article_id, count);

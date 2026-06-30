package com.samar.nami.repository;

import com.samar.nami.entity.InvertedIndex;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

public interface InvertedIndexRepository extends JpaRepository<InvertedIndex, Long> {

    // returns full entities (we need articleId + count for TF-IDF scoring)
    List<InvertedIndex> findByWord(String word);

    /**
     * Rebuild the word_stats(word, df) table in one bulk pass from inverted_index.
     * Called after a crawl/load batch rather than per-insert, so indexing stays fast.
     * Fast (~3.5s for ~835k words) because it's a single GROUP BY, not row-by-row.
     */
    @Modifying
    @Transactional
    @Query(value = """
        TRUNCATE word_stats;
        INSERT INTO word_stats (word, df)
        SELECT word, COUNT(*) FROM inverted_index GROUP BY word;
        """, nativeQuery = true)
    void rebuildWordStats();

    /** One ranked row from the SQL BM25 query: an article id and its summed score. */
    interface ScoredArticle {
        Long getArticleId();
        double getScore();
    }

    /**
     * BM25 ranking done ENTIRELY in Postgres, returning only the requested page.
     *
     * This replaces the old "fetch all postings into Java, score in a loop, sort"
     * approach. Postgres computes the score for every matching (word, article)
     * posting, sums per article, sorts, and ships back only :limit rows — so Java
     * never sees the (potentially hundreds of thousands of) matches.
     *
     * The formula matches the Java version exactly:
     *   idf = ln(1 + (N - df + 0.5) / (df + 0.5))                 (Lucene-safe IDF)
     *   bm25 = idf * (tf*(k1+1)) / (tf + k1*(1 - b + b*(len/avgDl)))
     * df (per word) is computed in SQL via COUNT(*) over that word's postings.
     *
     * :words = stemmed query words, :n = total articles, :avgDl = avg doc length,
     * :k1/:b = BM25 params, :limit/:offset = pagination (LIMIT/OFFSET).
     */
    @Query(value = """
        WITH matched AS (
            SELECT ii.article_id, ii.count AS tf, ii.word
            FROM inverted_index ii
            WHERE ii.word IN (:words)
        ),
        dfs AS (
            SELECT word, COUNT(*) AS df FROM matched GROUP BY word
        )
        SELECT m.article_id AS articleId,
               SUM(
                   ln(1 + (:n - d.df + 0.5) / (d.df + 0.5))
                   * (m.tf * (:k1 + 1))
                   / (m.tf + :k1 * (1 - :b + :b * (a.total_words::float / :avgDl)))
               ) AS score
        FROM matched m
        JOIN dfs d ON d.word = m.word
        JOIN article a ON a.id = m.article_id
        GROUP BY m.article_id
        ORDER BY score DESC
        LIMIT :limit OFFSET :offset
        """, nativeQuery = true)
    List<ScoredArticle> searchBm25(@Param("words") List<String> words,
                                   @Param("n") long n,
                                   @Param("avgDl") double avgDl,
                                   @Param("k1") double k1,
                                   @Param("b") double b,
                                   @Param("limit") int limit,
                                   @Param("offset") int offset);

    /**
     * Total number of DISTINCT articles matching any of the query words.
     * Needed for pagination metadata (totalResults / totalPages) since the
     * ranked query above only returns one page.
     */
    @Query(value = """
        SELECT COUNT(DISTINCT ii.article_id)
        FROM inverted_index ii
        WHERE ii.word IN (:words)
        """, nativeQuery = true)
    long countMatching(@Param("words") List<String> words);

    /**
     * Fuzzy fallback: find the single indexed word most similar to the input,
     * using pg_trgm trigram similarity. Returns null if nothing is similar enough.
     *
     * - "word % :input" keeps only words above pg_trgm's default similarity
     *   threshold (0.3), so obvious non-matches are discarded.
     * - df >= :minDf is the document-frequency floor: it discards ultra-rare words
     *   (rare names, junk tokens) that are "close" in spelling but useless as
     *   suggestions. df is read from word_stats (precomputed) instead of counting
     *   inverted_index live — that change cut this query from ~2s to ~10ms.
     * - ORDER BY similarity(...) DESC LIMIT 1 picks the closest spelling among
     *   the survivors.
     */
    @Query(value = """
        SELECT word
        FROM word_stats
        WHERE word % :input
          AND df >= :minDf
        ORDER BY similarity(word, :input) DESC
        LIMIT 1
        """, nativeQuery = true)
    String findNearestWord(@Param("input") String input, @Param("minDf") int minDf);
}

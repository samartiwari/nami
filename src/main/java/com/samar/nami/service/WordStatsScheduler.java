package com.samar.nami.service;

import com.samar.nami.repository.InvertedIndexRepository;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Periodically rebuilds the word_stats(word, df) table while the app runs.
 *
 * Fuzzy "did you mean" reads df from word_stats. During a long crawl (days),
 * waiting until the crawl finishes to rebuild would leave fuzzy search stale the
 * whole time. So we rebuild on a timer instead: every hour, word_stats catches up
 * to the newly-crawled words and suggestions stay reasonably fresh.
 *
 * A rebuild is one bulk TRUNCATE + GROUP BY (a few seconds), so running it hourly
 * is cheap even as the index grows.
 */
@Component
public class WordStatsScheduler {

    private static final long ONE_HOUR_MS = 60 * 60 * 1000L;

    private final InvertedIndexRepository invertedIndexRepository;

    public WordStatsScheduler(InvertedIndexRepository invertedIndexRepository) {
        this.invertedIndexRepository = invertedIndexRepository;
    }

    // fixedDelay = wait this long AFTER the previous rebuild finishes before the
    // next one, so rebuilds never overlap. initialDelay skips the first hour so
    // startup isn't slowed.
    @Scheduled(fixedDelay = ONE_HOUR_MS, initialDelay = ONE_HOUR_MS)
    public void rebuildWordStats() {
        System.out.println("Scheduled word_stats rebuild starting...");
        invertedIndexRepository.rebuildWordStats();
        System.out.println("Scheduled word_stats rebuild done.");
    }
}

package com.samar.nami.bootstrap;

import com.samar.nami.service.CrawlerService;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * Auto-runs the crawler on startup — but only when nami.crawler.enabled=true.
 * Set it to false (e.g. during dump-loading benchmarks) so the app boots without
 * crawling live Wikipedia in the background.
 */
@Component
@ConditionalOnProperty(name = "nami.crawler.enabled", havingValue = "true")
class CrawlerStarter implements CommandLineRunner {

    private final CrawlerService crawlerService;

    public CrawlerStarter (CrawlerService crawlerService){
        this.crawlerService = crawlerService;
    }

    @Override
    public void run(String... args) throws Exception {
        crawlerService.crawl("https://en.wikipedia.org/wiki/Lionel_Messi");
    }
}
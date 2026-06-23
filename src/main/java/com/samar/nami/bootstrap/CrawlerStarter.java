package com.samar.nami.bootstrap;

import com.samar.nami.service.CrawlerService;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

@Component
class CrawlerStarter implements CommandLineRunner {

    private final CrawlerService crawlerService;

    public CrawlerStarter (CrawlerService crawlerService){
        this.crawlerService = crawlerService;
    }

    @Override
    public void run(String... args) throws Exception {
        crawlerService.crawl("https://en.wikipedia.org/wiki/Lorem_ipsum");
    }
}
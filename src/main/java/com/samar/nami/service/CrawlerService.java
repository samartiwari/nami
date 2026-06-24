package com.samar.nami.service;

import com.samar.nami.entity.Article;
import com.samar.nami.entity.Frontier;
import com.samar.nami.repository.ArticleRepository;
import com.samar.nami.repository.FrontierRepository;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

@Service
public class CrawlerService {

    private static final String WIKI_PREFIX = "https://en.wikipedia.org/wiki/";
    private static final int PAGE_LIMIT = 100;
    private static final String PENDING = "PENDING";
    private static final String DONE = "DONE";

    private final ArticleRepository articleRepository;
    private final FrontierRepository frontierRepository;
    private final IndexingService indexingService;

    public CrawlerService(ArticleRepository articleRepository, FrontierRepository frontierRepository,
                          IndexingService indexingService) {
        this.articleRepository = articleRepository;
        this.frontierRepository = frontierRepository;
        this.indexingService = indexingService;
    }

    // The crawl loop: pulls PENDING urls from the frontier table, follows links.
    public void crawl(String seedUrl) throws InterruptedException {
        // in-memory cache of every url already in the frontier (loaded once at startup)
        // so we don't hit the DB to check "known?" for every link
        Set<String> known = new HashSet<>(frontierRepository.findAllUrls());

        // seed the frontier if this url isn't known yet (first run, or new seed)
        if (!known.contains(seedUrl)) {
            frontierRepository.save(new Frontier(seedUrl, PENDING));
            known.add(seedUrl);
        }

        int count = 0;

        while (count < PAGE_LIMIT) {
            Optional<Frontier> next = frontierRepository.findFirstByStatus(PENDING);
            if (next.isEmpty()) {
                break;   // nothing left to crawl
            }
            Frontier current = next.get();
            String url = current.getUrl();

            try {
                System.out.println("Crawling [" + (count + 1) + "]: " + url);
                PageData page = fetchPage(url);

                // save the article content (skip if somehow already stored)
                if (!articleRepository.existsByUrl(url)) {
                    Article saved = articleRepository.save(new Article(page.title, url, page.snippet));
                    // index the full page text (must happen after save — we need the generated ID)
                    indexingService.indexPage(saved.getId(), page.fullText);
                }

                // collect NEW links (in-memory dedup), then batch insert as PENDING
                List<Frontier> toInsert = new ArrayList<>();
                for (String link : page.links) {
                    if (!known.contains(link)) {
                        known.add(link);
                        toInsert.add(new Frontier(link, PENDING));
                    }
                }
                frontierRepository.saveAll(toInsert);

                // mark this url as crawled
                current.setStatus(DONE);
                frontierRepository.save(current);

            } catch (IOException e) {
                // one bad page should not crash the crawl — mark it DONE so we don't retry forever
                System.out.println("Skipping " + url + " — failed: " + e.getMessage());
                current.setStatus(DONE);
                frontierRepository.save(current);
            }

            count++;
            Thread.sleep(1000);   // politeness: 1 sec between fetches
        }

        System.out.println("Done. Crawled " + count + " pages. Pending left: "
                + frontierRepository.countByStatus(PENDING));
    }

    // Fetch ONE page: returns its title, snippet, and good article links.
    public PageData fetchPage(String url) throws IOException {
        Document doc = Jsoup.connect(url)
                .userAgent("namibot/0.1 (samartiwari2004@gmail.com)")
                .get();

        String title = doc.title();

        // snippet = first real content paragraph, capped at ~200 chars
        String snippet = "";
        for (Element p : doc.select("div.mw-parser-output > p")) {
            String text = p.text().trim();
            if (text.length() > 50) {
                snippet = text;
                break;
            }
        }
        if (snippet.length() > 200) {
            snippet = snippet.substring(0, 200) + "...";
        }

        // filter links to real Wikipedia articles (no ":" namespace)
        List<String> articleLinks = new ArrayList<>();
        for (Element link : doc.select("a[href]")) {
            String fullUrl = link.attr("abs:href");

            // strip "#section" anchors — they point to the SAME page, not a new one
            int hashIndex = fullUrl.indexOf("#");
            if (hashIndex != -1) {
                fullUrl = fullUrl.substring(0, hashIndex);
            }

            if (fullUrl.startsWith(WIKI_PREFIX)) {
                String titlePart = fullUrl.substring(WIKI_PREFIX.length());
                if (!titlePart.contains(":")) {
                    articleLinks.add(fullUrl);
                }
            }
        }

        // full text for indexing (not stored in DB — only used during the crawl)
        String fullText = doc.select("div.mw-parser-output").text();

        return new PageData(title, snippet, fullText, articleLinks);
    }

    // small holder for one page's extracted data
    public static class PageData {
        final String title;
        final String snippet;
        final String fullText;
        final List<String> links;

        PageData(String title, String snippet, String fullText, List<String> links) {
            this.title = title;
            this.snippet = snippet;
            this.fullText = fullText;
            this.links = links;
        }
    }
}

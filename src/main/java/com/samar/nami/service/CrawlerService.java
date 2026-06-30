package com.samar.nami.service;

import com.samar.nami.entity.Article;
import com.samar.nami.entity.Frontier;
import com.samar.nami.repository.ArticleRepository;
import com.samar.nami.repository.FrontierRepository;
import com.samar.nami.repository.InvertedIndexRepository;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
public class CrawlerService {

    private static final String WIKI_PREFIX = "https://en.wikipedia.org/wiki/";
    private static final int PAGE_LIMIT = 150_000;
    private static final String PENDING = "PENDING";
    private static final String DONE = "DONE";

    private final ArticleRepository articleRepository;
    private final FrontierRepository frontierRepository;
    private final IndexingService indexingService;
    private final InvertedIndexRepository invertedIndexRepository;

    public CrawlerService(ArticleRepository articleRepository, FrontierRepository frontierRepository,
                          IndexingService indexingService, InvertedIndexRepository invertedIndexRepository) {
        this.articleRepository = articleRepository;
        this.frontierRepository = frontierRepository;
        this.indexingService = indexingService;
        this.invertedIndexRepository = invertedIndexRepository;
    }

    // The crawl loop: pulls PENDING urls from the frontier table, follows links.
    public void crawl(String seedUrl) throws InterruptedException {
        // Dedup is handled by the DB (unique constraint on frontier.url + ON CONFLICT
        // DO NOTHING), so there is no in-memory "known" set to hold/rebuild. Seeding
        // is just an insert-if-new; a duplicate seed is silently skipped.
        frontierRepository.insertIfNew(seedUrl, PENDING);

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
                    int totalWords = page.fullText.split("\\W+").length;
                    Article saved = articleRepository.save(new Article(page.title, url, page.snippet, totalWords));
                    // index the full page text (must happen after save — we need the generated ID)
                    indexingService.indexPage(saved.getId(), page.fullText);
                }

                // insert each discovered link as PENDING; the DB skips ones already
                // seen (ON CONFLICT DO NOTHING), so no in-memory dedup is needed
                for (String link : page.links) {
                    frontierRepository.insertIfNew(link, PENDING);
                }

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

        // Refresh the precomputed df table so fuzzy "did you mean" reflects the
        // newly-crawled words. One bulk rebuild at the END of the run (not per page)
        // keeps indexing fast — df only needs to be fresh for search, not mid-crawl.
        System.out.println("Rebuilding word_stats (df) after crawl...");
        invertedIndexRepository.rebuildWordStats();

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

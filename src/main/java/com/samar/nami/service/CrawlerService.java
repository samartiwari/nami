package com.samar.nami.service;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Queue;
import java.util.Set;

@Service
public class CrawlerService {

    private static final String WIKI_PREFIX = "https://en.wikipedia.org/wiki/";
    private static final int PAGE_LIMIT = 5;

    // The crawl loop: queue + seen-set, follows links until the limit is hit.
    public void crawl(String seedUrl) throws InterruptedException {
        Queue<String> queue = new ArrayDeque<>();
        Set<String> seen = new HashSet<>();

        queue.add(seedUrl);
        seen.add(seedUrl);

        int count = 0;

        while (!queue.isEmpty() && count < PAGE_LIMIT) {
            String url = queue.poll();          // take the next URL (front of queue)

            try {
                System.out.println("Crawling [" + (count + 1) + "]: " + url);
                List<String> links = getArticleLinks(url);   // fetch + parse + filter

                for (String link : links) {
                    if (!seen.contains(link)) {     // not seen before?
                        seen.add(link);             // mark seen + enqueue together
                        queue.add(link);
                    }
                }
            } catch (IOException e) {
                // one bad page (404, timeout, etc.) should NOT crash the whole crawl
                System.out.println("Skipping " + url + " — failed: " + e.getMessage());
            }

            count++;
            Thread.sleep(1000);                 // politeness: 1 sec between fetches
        }

        System.out.println("Done. Crawled " + count + " pages, queue still has " + queue.size());
    }

    // Fetch ONE page and return its good (crawlable) article links.
    public List<String> getArticleLinks(String url) throws IOException {
        Document doc = Jsoup.connect(url)
                .userAgent("namibot/0.1 (samartiwari2004@gmail.com)")
                .get();

        String title = doc.title();
        Elements links = doc.select("a[href]");

        System.out.println("Title: " + title);

        List<String> articleLinks = new ArrayList<>();
        for (Element link : links) {
            String fullUrl = link.attr("abs:href");

            // keep only real Wikipedia articles: must be a /wiki/ URL,
            // and the title part (after /wiki/) must have no ":" namespace
            if (fullUrl.startsWith(WIKI_PREFIX)) {
                String titlePart = fullUrl.substring(WIKI_PREFIX.length());
                if (!titlePart.contains(":")) {
                    articleLinks.add(fullUrl);
                }
            }
        }

        System.out.println("Number of correct links: " + articleLinks.size());
        return articleLinks;
    }

}

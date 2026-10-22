package com.dung.seoanalyzer.seoanalyzer.service;

import java.io.IOException;
import java.net.URI;
import java.util.List;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.stereotype.Service;

import com.dung.seoanalyzer.seoanalyzer.dto.SeoPageData;

@Service
public class CrawlerService {

    private static final String USER_AGENT = "Mozilla/5.0 (compatible; SeoAnalyzerBot/1.0)";
    private static final int TIMEOUT_MS = 10_000;

    // Cách 1: crawl trực tiếp từ URL
    public SeoPageData crawl(String url) throws IOException {
        Document doc = Jsoup.connect(url)
                .userAgent(USER_AGENT)
                .timeout(TIMEOUT_MS)
                .get();
        return extract(doc);
    }

    // Cách 2: phân tích HTML do người dùng dán vào (dự phòng khi site chặn crawl)
    public SeoPageData parseHtml(String html, String baseUrl) {
        Document doc = Jsoup.parse(html, baseUrl == null ? "" : baseUrl);
        return extract(doc);
    }

    private SeoPageData extract(Document doc) {
        String title = doc.title();
        String metaDescription = doc.select("meta[name=description]").attr("content");
        String canonical = doc.select("link[rel=canonical]").attr("abs:href");
        String lang = doc.select("html").attr("lang");

        List<String> h1List = doc.select("h1").eachText();
        int h2Count = doc.select("h2").size();
        int h3Count = doc.select("h3").size();

        String bodyText = doc.body() != null ? doc.body().text().trim() : "";
        int wordCount = bodyText.isEmpty() ? 0 : bodyText.split("\\s+").length;

        Elements images = doc.select("img");
        int imagesMissingAlt = (int) images.stream()
                .filter(img -> img.attr("alt").isBlank())
                .count();

        String pageHost = normalizeHost(doc.location());
        int internalLinks = 0;
        int externalLinks = 0;
        for (Element link : doc.select("a[href]")) {
            String href = link.attr("abs:href");
            if (!href.startsWith("http")) continue; // bỏ qua mailto:, tel:, javascript:
            if (pageHost != null && pageHost.equals(normalizeHost(href))) {
                internalLinks++;
            } else {
                externalLinks++;
            }
        }

        return new SeoPageData(doc.location(), title, metaDescription, canonical, lang,
                h1List, h2Count, h3Count, wordCount,
                images.size(), imagesMissingAlt, internalLinks, externalLinks);
    }

    // Chuẩn hoá host: bỏ "www." để www.abc.com và abc.com được coi là cùng 1 site
    private String normalizeHost(String url) {
        try {
            String host = URI.create(url).getHost();
            if (host == null) return null;
            host = host.toLowerCase();
            return host.startsWith("www.") ? host.substring(4) : host;
        } catch (IllegalArgumentException e) {
            return null;
        }
    }
}
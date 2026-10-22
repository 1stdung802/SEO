package com.dung.seoanalyzer.seoanalyzer.dto;

import java.util.List;

/**
 * Dữ liệu SEO on-page đã trích xuất từ một trang.
 *
 * @param coreWebVitals chỉ số tốc độ từ PageSpeed Insights; null nếu chưa đo
 */
public record SeoPageData(
        String url,
        String title,
        String metaDescription,
        String canonical,
        String lang,
        List<String> h1List,
        int h2Count,
        int h3Count,
        int wordCount,
        int totalImages,
        int imagesMissingAlt,
        int internalLinks,
        int externalLinks,
        CoreWebVitals coreWebVitals
) {
    /** Constructor cũ (13 tham số) dùng trong CrawlerService: chưa có Core Web Vitals. */
    public SeoPageData(String url, String title, String metaDescription, String canonical, String lang,
                       List<String> h1List, int h2Count, int h3Count, int wordCount,
                       int totalImages, int imagesMissingAlt, int internalLinks, int externalLinks) {
        this(url, title, metaDescription, canonical, lang, h1List, h2Count, h3Count, wordCount,
                totalImages, imagesMissingAlt, internalLinks, externalLinks, null);
    }

    /** Tạo bản sao có gắn thêm Core Web Vitals (record không sửa được nên tạo bản mới). */
    public SeoPageData withCoreWebVitals(CoreWebVitals vitals) {
        return new SeoPageData(url, title, metaDescription, canonical, lang, h1List, h2Count, h3Count,
                wordCount, totalImages, imagesMissingAlt, internalLinks, externalLinks, vitals);
    }
}

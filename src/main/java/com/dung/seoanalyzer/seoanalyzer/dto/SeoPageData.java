package com.dung.seoanalyzer.seoanalyzer.dto;

import java.util.List;

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
        int externalLinks
) {}
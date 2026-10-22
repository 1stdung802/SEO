package com.dung.seoanalyzer.seoanalyzer.controller;

import com.dung.seoanalyzer.seoanalyzer.dto.SeoPageData;
import com.dung.seoanalyzer.seoanalyzer.service.CrawlerService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;

/**
 * Controller kiểm thử Module 1: chỉ trích xuất dữ liệu SEO thô, không chấm điểm, không lưu.
 * Đường dẫn /api/analyze đã chuyển sang AnalysisApiController.
 */
@RestController
public class CrawlTestController {

    private final CrawlerService crawlerService;

    public CrawlTestController(CrawlerService crawlerService) {
        this.crawlerService = crawlerService;
    }

    @GetMapping("/api/crawl")
    public SeoPageData crawl(@RequestParam String url) throws IOException {
        return crawlerService.crawl(url);
    }
}

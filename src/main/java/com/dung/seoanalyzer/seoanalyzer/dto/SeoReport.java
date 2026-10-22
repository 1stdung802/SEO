package com.dung.seoanalyzer.seoanalyzer.dto;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Báo cáo tổng hợp của một lần phân tích SEO.
 *
 * @param url        URL được phân tích
 * @param analyzedAt thời điểm phân tích (kết quả chỉ đúng tại thời điểm này)
 * @param totalScore điểm tổng, thang 100
 * @param results    kết quả từng tiêu chí
 * @param pageData   dữ liệu thô đã trích xuất từ trang
 */
public record SeoReport(
        String url,
        LocalDateTime analyzedAt,
        int totalScore,
        List<CriterionResult> results,
        SeoPageData pageData
) {}

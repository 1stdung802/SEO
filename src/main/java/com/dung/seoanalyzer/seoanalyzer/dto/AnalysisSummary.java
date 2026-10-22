package com.dung.seoanalyzer.seoanalyzer.dto;

import java.time.LocalDateTime;

/**
 * Thông tin tóm tắt một lần phân tích, dùng cho danh sách lịch sử.
 */
public record AnalysisSummary(
        Long id,
        String url,
        String title,
        LocalDateTime analyzedAt,
        int totalScore
) {}

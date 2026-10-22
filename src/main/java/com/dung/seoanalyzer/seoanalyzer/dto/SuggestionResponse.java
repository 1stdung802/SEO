package com.dung.seoanalyzer.seoanalyzer.dto;

import java.time.LocalDateTime;

/**
 * Gợi ý cải thiện SEO do AI sinh cho một lần phân tích.
 *
 * @param cached true nếu lấy lại gợi ý đã lưu, false nếu vừa gọi AI sinh mới
 */
public record SuggestionResponse(
        Long analysisId,
        String url,
        int totalScore,
        String suggestion,
        LocalDateTime generatedAt,
        String model,
        boolean cached
) {}

package com.dung.seoanalyzer.seoanalyzer.dto.view;

import java.util.List;

/**
 * Toàn bộ dữ liệu đã định dạng sẵn cho trang kết quả phân tích.
 * Template Thymeleaf chỉ hiển thị, không tính toán.
 */
public record ResultView(
        Long id,
        String url,
        String analyzedAtText,

        // Điểm tổng và đồng hồ điểm
        int totalScore,
        String band,
        String bandLabel,
        String gaugeDash,

        // Khung mô phỏng kết quả tìm kiếm Google
        String serpUrl,
        String serpTitle,
        boolean titleTruncated,
        int titleLength,
        String serpMeta,
        boolean metaTruncated,
        boolean metaKnown,
        int metaLength,

        // Bảng tiêu chí
        int passedCount,
        int partialCount,
        int failedCount,
        List<CriterionRow> criteria,

        // Gợi ý AI (null nếu chưa tạo)
        String suggestion,
        String suggestionAtText
) {
    public boolean hasSuggestion() {
        return suggestion != null && !suggestion.isBlank();
    }

    public boolean hasFailures() {
        return partialCount + failedCount > 0;
    }
}

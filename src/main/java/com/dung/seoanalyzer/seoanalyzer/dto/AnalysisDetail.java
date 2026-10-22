package com.dung.seoanalyzer.seoanalyzer.dto;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Chi tiết một lần phân tích.
 *
 * @param pageData dữ liệu thô của trang; chỉ có khi vừa phân tích xong,
 *                 bằng null khi xem lại từ lịch sử (lịch sử chỉ lưu kết quả chấm điểm)
 */
public record AnalysisDetail(
        Long id,
        String url,
        String title,
        LocalDateTime analyzedAt,
        int totalScore,
        List<CriterionResult> results,
        SeoPageData pageData
) {}

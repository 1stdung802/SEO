package com.dung.seoanalyzer.seoanalyzer.dto;

/**
 * Kết quả chấm điểm của một tiêu chí SEO.
 *
 * @param code    mã tiêu chí (VD: TITLE)
 * @param name    tên hiển thị
 * @param weight  trọng số của tiêu chí
 * @param score   1 = đạt, 0.5 = đạt một phần, 0 = không đạt
 * @param message thông báo giải thích kết quả
 * @param source  căn cứ lý thuyết của tiêu chí
 */
public record CriterionResult(
        String code,
        String name,
        int weight,
        double score,
        String message,
        String source
) {
    /** Số điểm thực nhận = trọng số x mức đạt. */
    public double earnedPoints() {
        return weight * score;
    }
}

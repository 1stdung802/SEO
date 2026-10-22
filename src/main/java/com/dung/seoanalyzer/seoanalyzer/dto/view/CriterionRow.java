package com.dung.seoanalyzer.seoanalyzer.dto.view;

/**
 * Một dòng trong bảng tiêu chí trên trang kết quả.
 *
 * @param status      pass | partial | fail (dùng làm class CSS)
 * @param statusLabel nhãn hiển thị: Đạt / Đạt một phần / Chưa đạt
 * @param icon        biểu tượng (✓, !, ✕) đi kèm, để trạng thái không chỉ phân biệt bằng màu
 * @param earnedText  điểm nhận được trên trọng số, VD "7,5 / 15"
 */
public record CriterionRow(
        String name,
        int weight,
        String earnedText,
        String status,
        String statusLabel,
        String icon,
        String message,
        String source
) {}

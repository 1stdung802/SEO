package com.dung.seoanalyzer.seoanalyzer.dto.view;

/**
 * Một dòng trong danh sách lịch sử phân tích.
 *
 * @param band good | fair | poor, quyết định màu của điểm
 */
public record HistoryRow(
        Long id,
        String url,
        String title,
        String analyzedAtText,
        int totalScore,
        String band,
        String bandLabel
) {}

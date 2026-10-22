package com.dung.seoanalyzer.seoanalyzer.service;

import com.dung.seoanalyzer.seoanalyzer.criteria.SeoCriterion;
import com.dung.seoanalyzer.seoanalyzer.dto.CriterionResult;
import com.dung.seoanalyzer.seoanalyzer.dto.SeoPageData;
import com.dung.seoanalyzer.seoanalyzer.dto.SeoReport;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Chấm điểm SEO on-page bằng cách chạy lần lượt tất cả tiêu chí.
 * Spring tự động truyền vào mọi class @Component implement SeoCriterion,
 * theo thứ tự @Order, nên thêm tiêu chí mới không cần sửa class này.
 */
@Service
public class SeoScoringService {

    private final List<SeoCriterion> criteria;

    public SeoScoringService(List<SeoCriterion> criteria) {
        this.criteria = criteria;
    }

    public SeoReport score(SeoPageData data) {
        List<CriterionResult> results = criteria.stream()
                .map(criterion -> criterion.evaluate(data))
                .toList();

        // Quy về thang 100 theo tổng trọng số, không phụ thuộc số lượng tiêu chí
        int maxPoints = results.stream().mapToInt(CriterionResult::weight).sum();
        double earned = results.stream().mapToDouble(CriterionResult::earnedPoints).sum();
        int totalScore = maxPoints == 0 ? 0 : (int) Math.round(earned / maxPoints * 100);

        return new SeoReport(data.url(), LocalDateTime.now(), totalScore, results, data);
    }
}

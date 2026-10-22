package com.dung.seoanalyzer.seoanalyzer.criteria;

import com.dung.seoanalyzer.seoanalyzer.dto.CriterionResult;
import com.dung.seoanalyzer.seoanalyzer.dto.SeoPageData;

public interface SeoCriterion {
    String code();
    String name();
    int weight();
    String source();
    CriterionResult evaluate(SeoPageData data);

    // Hàm tiện ích: các class con chỉ cần truyền điểm và thông báo
    default CriterionResult result(double score, String message) {
        return new CriterionResult(code(), name(), weight(), score, message, source());
    }
}
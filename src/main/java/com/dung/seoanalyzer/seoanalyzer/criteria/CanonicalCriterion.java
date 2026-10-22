package com.dung.seoanalyzer.seoanalyzer.criteria;

import com.dung.seoanalyzer.seoanalyzer.dto.CriterionResult;
import com.dung.seoanalyzer.seoanalyzer.dto.SeoPageData;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

/**
 * Tiêu chí 6 - Thẻ canonical.
 * <p>
 * Căn cứ: Google Search Central - hướng dẫn chuẩn hoá URL trùng lặp. Thẻ canonical
 * cho Google biết đâu là phiên bản chính của trang khi cùng nội dung có nhiều URL.
 * <p>
 * Thang điểm: 1 = có canonical dạng URL hợp lệ; 0.5 = có nhưng không phải URL http(s);
 * 0 = không khai báo.
 */
@Component
@Order(6)
public class CanonicalCriterion implements SeoCriterion {

    @Override
    public String code() {
        return "CANONICAL";
    }

    @Override
    public String name() {
        return "Thẻ canonical";
    }

    @Override
    public int weight() {
        return 10;
    }

    @Override
    public String source() {
        return "Google Search Central: chuẩn hoá URL trùng lặp";
    }

    @Override
    public CriterionResult evaluate(SeoPageData data) {
        String canonical = data.canonical() == null ? "" : data.canonical().strip();
        if (canonical.isEmpty()) {
            return result(0, "Trang chưa khai báo thẻ canonical.");
        }
        if (!canonical.startsWith("http://") && !canonical.startsWith("https://")) {
            return result(0.5, "Canonical không phải URL đầy đủ: " + canonical);
        }
        return result(1, "Có canonical: " + canonical);
    }
}
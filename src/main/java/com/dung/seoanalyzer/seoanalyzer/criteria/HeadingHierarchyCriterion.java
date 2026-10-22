package com.dung.seoanalyzer.seoanalyzer.criteria;

import com.dung.seoanalyzer.seoanalyzer.dto.CriterionResult;
import com.dung.seoanalyzer.seoanalyzer.dto.SeoPageData;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

/**
 * Tiêu chí 4 - Phân cấp heading.
 * <p>
 * Căn cứ: thực tiễn ngành về cấu trúc nội dung. Heading nên đi theo thứ bậc
 * H1 → H2 → H3, không nhảy cấp (VD: có H3 nhưng không có H2).
 * <p>
 * Thang điểm: 1 = có H2 và không nhảy cấp; 0.5 = chưa có heading phụ; 0 = nhảy cấp.
 */
@Component
@Order(4)
public class HeadingHierarchyCriterion implements SeoCriterion {

    @Override
    public String code() {
        return "HEADING_HIERARCHY";
    }

    @Override
    public String name() {
        return "Phân cấp heading";
    }

    @Override
    public int weight() {
        return 10;
    }

    @Override
    public String source() {
        return "Thực tiễn ngành về cấu trúc nội dung";
    }

    @Override
    public CriterionResult evaluate(SeoPageData data) {
        int h2 = data.h2Count();
        int h3 = data.h3Count();

        if (h2 == 0 && h3 > 0) {
            return result(0, "Có " + h3 + " thẻ H3 nhưng không có H2, cấu trúc heading bị nhảy cấp.");
        }
        if (h2 == 0) {
            return result(0.5, "Chưa có heading phụ (H2) để chia nhỏ nội dung.");
        }
        return result(1, "Cấu trúc heading hợp lý (" + h2 + " H2, " + h3 + " H3).");
    }
}
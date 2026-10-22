package com.dung.seoanalyzer.seoanalyzer.criteria;

import com.dung.seoanalyzer.seoanalyzer.dto.CriterionResult;
import com.dung.seoanalyzer.seoanalyzer.dto.SeoPageData;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

/**
 * Tiêu chí 7 - Liên kết nội bộ.
 * <p>
 * Căn cứ: Google SEO Starter Guide - cấu trúc liên kết giúp Google khám phá (crawl)
 * và hiểu mối quan hệ giữa các trang trong cùng website.
 * <p>
 * Thang điểm: 1 = có ít nhất 1 liên kết nội bộ; 0 = không có.
 */
@Component
@Order(7)
public class InternalLinkCriterion implements SeoCriterion {

    @Override
    public String code() {
        return "INTERNAL_LINKS";
    }

    @Override
    public String name() {
        return "Liên kết nội bộ";
    }

    @Override
    public int weight() {
        return 15;
    }

    @Override
    public String source() {
        return "Google SEO Starter Guide: cấu trúc liên kết giúp crawl";
    }

    @Override
    public CriterionResult evaluate(SeoPageData data) {
        int internal = data.internalLinks();
        if (internal <= 0) {
            return result(0, "Trang không có liên kết nội bộ nào, Google khó khám phá các trang khác của website.");
        }
        return result(1, "Có " + internal + " liên kết nội bộ (và " + data.externalLinks() + " liên kết ngoài).");
    }
}
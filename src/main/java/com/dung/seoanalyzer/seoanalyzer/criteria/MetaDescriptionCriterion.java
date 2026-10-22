package com.dung.seoanalyzer.seoanalyzer.criteria;

import com.dung.seoanalyzer.seoanalyzer.dto.CriterionResult;
import com.dung.seoanalyzer.seoanalyzer.dto.SeoPageData;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

/**
 * Tiêu chí 2 - Thẻ mô tả (meta description).
 * <p>
 * Căn cứ: Google SEO Starter Guide khuyến nghị mô tả tóm tắt, chính xác nội dung trang,
 * vì Google có thể dùng nó làm đoạn trích (snippet) trên kết quả tìm kiếm.
 * Ngưỡng 70-160 ký tự lấy từ thực tiễn ngành (Moz).
 * <p>
 * Thang điểm: 1 = có và đúng độ dài; 0.5 = có nhưng quá ngắn/dài; 0 = không có.
 */
@Component
@Order(2)
public class MetaDescriptionCriterion implements SeoCriterion {

    private static final int MIN_LENGTH = 70;
    private static final int MAX_LENGTH = 160;

    @Override
    public String code() {
        return "META_DESC";
    }

    @Override
    public String name() {
        return "Thẻ mô tả (meta description)";
    }

    @Override
    public int weight() {
        return 15;
    }

    @Override
    public String source() {
        return "Google SEO Starter Guide; ngưỡng độ dài: Moz";
    }

    @Override
    public CriterionResult evaluate(SeoPageData data) {
        String description = data.metaDescription() == null ? "" : data.metaDescription().strip();
        if (description.isEmpty()) {
            return result(0, "Trang chưa có meta description.");
        }

        int length = description.codePointCount(0, description.length());
        if (length < MIN_LENGTH) {
            return result(0.5, "Meta description quá ngắn (" + length + " ký tự), nên dài "
                    + MIN_LENGTH + "–" + MAX_LENGTH + " ký tự.");
        }
        if (length > MAX_LENGTH) {
            return result(0.5, "Meta description quá dài (" + length + " ký tự), có thể bị cắt trên kết quả tìm kiếm. "
                    + "Nên dài " + MIN_LENGTH + "–" + MAX_LENGTH + " ký tự.");
        }
        return result(1, "Độ dài meta description hợp lý (" + length + " ký tự).");
    }
}
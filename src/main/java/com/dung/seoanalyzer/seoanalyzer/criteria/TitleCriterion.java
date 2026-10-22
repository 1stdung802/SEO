package com.dung.seoanalyzer.seoanalyzer.criteria;

import com.dung.seoanalyzer.seoanalyzer.dto.CriterionResult;
import com.dung.seoanalyzer.seoanalyzer.dto.SeoPageData;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

/**
 * Tiêu chí 1 - Thẻ tiêu đề (title).
 * <p>
 * Căn cứ: Google SEO Starter Guide khuyến nghị tiêu đề mô tả rõ nội dung và ngắn gọn.
 * Ngưỡng 30-60 ký tự lấy từ thực tiễn ngành (Moz), vì Google không công bố con số cụ thể.
 * <p>
 * Thang điểm: 1 = có và đúng độ dài; 0.5 = có nhưng quá ngắn/dài; 0 = không có.
 */
@Component
@Order(1)
public class TitleCriterion implements SeoCriterion {

    private static final int MIN_LENGTH = 30;
    private static final int MAX_LENGTH = 60;

    @Override
    public String code() {
        return "TITLE";
    }

    @Override
    public String name() {
        return "Thẻ tiêu đề (title)";
    }

    @Override
    public int weight() {
        return 20;
    }

    @Override
    public String source() {
        return "Google SEO Starter Guide; ngưỡng độ dài: Moz";
    }

    @Override
    public CriterionResult evaluate(SeoPageData data) {
        String title = data.title() == null ? "" : data.title().strip();
        if (title.isEmpty()) {
            return result(0, "Trang chưa có thẻ title.");
        }

        // Đếm theo ký tự thực (code point) để xử lý đúng cả ký tự đặc biệt
        int length = title.codePointCount(0, title.length());
        if (length < MIN_LENGTH) {
            return result(0.5, "Title quá ngắn (" + length + " ký tự), nên dài "
                    + MIN_LENGTH + "–" + MAX_LENGTH + " ký tự.");
        }
        if (length > MAX_LENGTH) {
            return result(0.5, "Title quá dài (" + length + " ký tự), có thể bị cắt trên kết quả tìm kiếm. "
                    + "Nên dài " + MIN_LENGTH + "–" + MAX_LENGTH + " ký tự.");
        }
        return result(1, "Độ dài title hợp lý (" + length + " ký tự).");
    }
}
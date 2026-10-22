package com.dung.seoanalyzer.seoanalyzer.criteria;

import com.dung.seoanalyzer.seoanalyzer.dto.CriterionResult;
import com.dung.seoanalyzer.seoanalyzer.dto.SeoPageData;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Tiêu chí 3 - Thẻ H1.
 * <p>
 * Căn cứ: Google SEO Starter Guide khuyến nghị dùng heading để thể hiện cấu trúc nội dung.
 * H1 là tiêu đề chính hiển thị trên trang, giúp người đọc và công cụ tìm kiếm hiểu chủ đề.
 * <p>
 * Lưu ý: Google không phạt trang có nhiều H1, nên nhiều H1 vẫn được tính đạt,
 * chỉ kèm lời khuyên. H1 rỗng (không có chữ) không được tính.
 * <p>
 * Thang điểm: 1 = có ít nhất 1 H1 có nội dung; 0 = không có.
 */
@Component
@Order(3)
public class H1Criterion implements SeoCriterion {

    @Override
    public String code() {
        return "H1";
    }

    @Override
    public String name() {
        return "Thẻ H1";
    }

    @Override
    public int weight() {
        return 15;
    }

    @Override
    public String source() {
        return "Google SEO Starter Guide: heading thể hiện cấu trúc nội dung";
    }

    @Override
    public CriterionResult evaluate(SeoPageData data) {
        List<String> h1List = data.h1List() == null ? List.of() : data.h1List();
        long count = h1List.stream()
                .filter(text -> text != null && !text.isBlank())
                .count();

        if (count == 0) {
            return result(0, "Trang chưa có thẻ H1 (hoặc H1 không có nội dung).");
        }
        if (count > 1) {
            return result(1, "Trang có " + count + " thẻ H1. Vẫn hợp lệ, nhưng nên giữ 1 H1 "
                    + "để chủ đề chính của trang rõ ràng hơn.");
        }
        return result(1, "Trang có đúng 1 thẻ H1.");
    }
}
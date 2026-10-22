package com.dung.seoanalyzer.seoanalyzer.criteria;

import com.dung.seoanalyzer.seoanalyzer.dto.CriterionResult;
import com.dung.seoanalyzer.seoanalyzer.dto.SeoPageData;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

/**
 * Tiêu chí 5 - Thuộc tính alt của ảnh.
 * <p>
 * Căn cứ: Google Image SEO Guidelines - alt text giúp Google hiểu nội dung ảnh,
 * đồng thời hỗ trợ người dùng trình đọc màn hình.
 * <p>
 * Hạn chế đã biết: ảnh trang trí có thể để alt="" một cách hợp lệ, nhưng hệ thống
 * vẫn tính là thiếu alt. Ngoài ra, ảnh tải bằng JavaScript (lazy-load) không được đếm.
 * <p>
 * Thang điểm: 1 = từ 90% ảnh có alt; 0.5 = từ 50% đến dưới 90%; 0 = dưới 50%.
 * Trang không có ảnh: tính đạt (không áp dụng).
 */
@Component
@Order(5)
public class ImageAltCriterion implements SeoCriterion {

    private static final double PASS_RATIO = 0.9;
    private static final double PARTIAL_RATIO = 0.5;

    @Override
    public String code() {
        return "IMAGE_ALT";
    }

    @Override
    public String name() {
        return "Thuộc tính alt của ảnh";
    }

    @Override
    public int weight() {
        return 15;
    }

    @Override
    public String source() {
        return "Google Image SEO Guidelines";
    }

    @Override
    public CriterionResult evaluate(SeoPageData data) {
        int total = data.totalImages();
        if (total <= 0) {
            return result(1, "Trang không có ảnh, tiêu chí không áp dụng.");
        }

        // Giới hạn trong [0, total] để tránh số liệu bất thường làm sai tỷ lệ
        int missing = Math.max(0, Math.min(data.imagesMissingAlt(), total));
        double ratio = (double) (total - missing) / total;
        int percent = (int) Math.round(ratio * 100);
        String detail = " (" + (total - missing) + "/" + total + " ảnh)";

        if (ratio >= PASS_RATIO) {
            return result(1, percent + "% ảnh có alt" + detail + ".");
        }
        if (ratio >= PARTIAL_RATIO) {
            return result(0.5, "Chỉ " + percent + "% ảnh có alt" + detail + ", nên đạt từ 90%.");
        }
        return result(0, "Chỉ " + percent + "% ảnh có alt" + detail + ", cần bổ sung alt cho ảnh.");
    }
}
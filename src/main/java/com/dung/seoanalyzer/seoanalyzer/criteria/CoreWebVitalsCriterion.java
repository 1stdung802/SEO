package com.dung.seoanalyzer.seoanalyzer.criteria;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import com.dung.seoanalyzer.seoanalyzer.dto.CoreWebVitals;
import com.dung.seoanalyzer.seoanalyzer.dto.CriterionResult;
import com.dung.seoanalyzer.seoanalyzer.dto.SeoPageData;

/**
 * Tiêu chí 8 - Core Web Vitals (tốc độ và trải nghiệm tải trang).
 * <p>
 * Căn cứ: ngưỡng chính thức của Google (web.dev), đánh giá tại phân vị thứ 75:
 * <ul>
 *   <li>LCP: tốt ≤ 2,5 giây; cần cải thiện ≤ 4 giây; kém &gt; 4 giây</li>
 *   <li>INP: tốt ≤ 200 ms; cần cải thiện ≤ 500 ms; kém &gt; 500 ms</li>
 *   <li>CLS: tốt ≤ 0,1; cần cải thiện ≤ 0,25; kém &gt; 0,25</li>
 * </ul>
 * Thang điểm: đạt cả 3 chỉ số thì được 1; ngược lại lấy trung bình từng chỉ số
 * (tốt = 1, cần cải thiện = 0,5, kém = 0).
 * <p>
 * Nếu không đo được (thiếu API key, lỗi mạng, trang không có dữ liệu), tiêu chí có
 * trọng số 0, tức là không tính vào điểm tổng, để trang không bị trừ điểm oan.
 */
@Component
@Order(8)
public class CoreWebVitalsCriterion implements SeoCriterion {

    static final long LCP_GOOD = 2500, LCP_POOR = 4000;
    static final long INP_GOOD = 200, INP_POOR = 500;
    static final double CLS_GOOD = 0.1, CLS_POOR = 0.25;

    @Override
    public String code() {
        return "CORE_WEB_VITALS";
    }

    @Override
    public String name() {
        return "Core Web Vitals (tốc độ tải trang)";
    }

    @Override
    public int weight() {
        return 15;
    }

    @Override
    public String source() {
        return "Google Core Web Vitals (web.dev), đo bằng PageSpeed Insights";
    }

    @Override
    public CriterionResult evaluate(SeoPageData data) {
        CoreWebVitals v = data.coreWebVitals();
        if (v == null || !v.available()) {
            String reason = v == null ? "Chưa đo tốc độ trang." : v.unavailableReason();
            return new CriterionResult(code(), name(), 0, 0,
                    "Không đo được: " + reason + " Tiêu chí này không được tính vào điểm.", source());
        }

        List<Double> ratings = new ArrayList<>();
        List<String> parts = new ArrayList<>();

        if (v.lcpMs() != null) {
            double r = rate(v.lcpMs(), LCP_GOOD, LCP_POOR);
            ratings.add(r);
            parts.add("LCP " + format(v.lcpMs() / 1000.0, 1) + " giây (" + label(r) + ")");
        }
        if (v.inpMs() != null) {
            double r = rate(v.inpMs(), INP_GOOD, INP_POOR);
            ratings.add(r);
            parts.add("INP " + v.inpMs() + " ms (" + label(r) + ")");
        }
        if (v.cls() != null) {
            double r = rate(v.cls(), CLS_GOOD, CLS_POOR);
            ratings.add(r);
            parts.add("CLS " + format(v.cls(), 2) + " (" + label(r) + ")");
        }

        if (ratings.isEmpty()) {
            return new CriterionResult(code(), name(), 0, 0,
                    "Không có chỉ số nào đo được. Tiêu chí này không được tính vào điểm.", source());
        }

        boolean allGood = ratings.stream().allMatch(r -> r == 1.0);
        double score = allGood ? 1.0 : ratings.stream().mapToDouble(Double::doubleValue).average().orElse(0);

        StringBuilder message = new StringBuilder(String.join(", ", parts)).append('.');
        message.append(" Nguồn: ").append(v.source().description()).append(", thiết bị di động.");
        if (v.inpMs() == null) {
            message.append(" INP chỉ có trong dữ liệu người dùng thật nên không được đánh giá.");
        }
        return result(score, message.toString());
    }

    /** Xếp loại một chỉ số: 1 = tốt, 0,5 = cần cải thiện, 0 = kém. */
    static double rate(double value, double good, double poor) {
        if (value <= good) return 1.0;
        if (value <= poor) return 0.5;
        return 0.0;
    }

    private static String label(double rating) {
        if (rating == 1.0) return "tốt";
        if (rating == 0.5) return "cần cải thiện";
        return "kém";
    }

    private static String format(double value, int decimals) {
        return String.format(Locale.ROOT, "%." + decimals + "f", value).replace('.', ',');
    }
}

package com.dung.seoanalyzer.seoanalyzer.service;

import com.dung.seoanalyzer.seoanalyzer.dto.CoreWebVitals;
import com.dung.seoanalyzer.seoanalyzer.dto.CoreWebVitals.Source;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

import java.net.http.HttpClient;
import java.time.Duration;
import java.util.Map;

/**
 * Lấy chỉ số Core Web Vitals từ Google PageSpeed Insights API v5.
 * <p>
 * Thứ tự ưu tiên nguồn dữ liệu:
 * <ol>
 *   <li>Dữ liệu người dùng thật (CrUX) của chính URL</li>
 *   <li>Dữ liệu người dùng thật của cả tên miền, nếu URL chưa đủ lượt truy cập</li>
 *   <li>Kết quả đo mô phỏng của Lighthouse, nếu không có dữ liệu thật</li>
 * </ol>
 * Phương thức {@link #measure} không bao giờ ném lỗi: khi thất bại sẽ trả về
 * {@link CoreWebVitals#unavailable}, để lỗi đo tốc độ không làm hỏng cả lần phân tích.
 */
@Service
public class PageSpeedClient {

    private static final String BASE_URL = "https://www.googleapis.com/pagespeedonline/v5";

    private final String apiKey;
    private final String strategy;
    private final RestClient restClient;

    public PageSpeedClient(@Value("${pagespeed.api.key:}") String apiKey,
                           @Value("${pagespeed.strategy:mobile}") String strategy,
                           @Value("${pagespeed.timeout-seconds:90}") int timeoutSeconds) {
        this.apiKey = apiKey == null ? "" : apiKey.strip();
        this.strategy = strategy;

        HttpClient httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .build();
        JdkClientHttpRequestFactory requestFactory = new JdkClientHttpRequestFactory(httpClient);
        requestFactory.setReadTimeout(Duration.ofSeconds(timeoutSeconds));

        this.restClient = RestClient.builder()
                .baseUrl(BASE_URL)
                .requestFactory(requestFactory)
                .build();
    }

    public CoreWebVitals measure(String url) {
        if (apiKey.isEmpty()) {
            return CoreWebVitals.unavailable("Chưa cấu hình pagespeed.api.key trong secret.properties.");
        }
        try {
            Map<String, Object> response = restClient.get()
                    .uri("/runPagespeed?url={url}&strategy={strategy}&category=performance&key={key}",
                            url, strategy, apiKey)
                    .retrieve()
                    .body(new ParameterizedTypeReference<Map<String, Object>>() {});
            return parse(response);
        } catch (RestClientResponseException e) {
            int status = e.getStatusCode().value();
            String reason = switch (status) {
                case 429 -> "Đã vượt hạn mức của PageSpeed Insights API, hãy thử lại sau.";
                case 400 -> "PageSpeed Insights không tải được trang này (trang lỗi, chặn truy cập hoặc URL không hợp lệ).";
                case 403 -> "API key PageSpeed không hợp lệ hoặc chưa bật PageSpeed Insights API.";
                default -> "PageSpeed Insights trả về lỗi " + status + ".";
            };
            return CoreWebVitals.unavailable(reason);
        } catch (ResourceAccessException e) {
            return CoreWebVitals.unavailable("PageSpeed Insights phản hồi quá lâu hoặc mất kết nối.");
        } catch (RuntimeException e) {
            return CoreWebVitals.unavailable("Không đọc được kết quả từ PageSpeed Insights.");
        }
    }

    /** Đọc phản hồi JSON (dạng Map) thành CoreWebVitals theo thứ tự ưu tiên nguồn dữ liệu. */
    static CoreWebVitals parse(Map<String, Object> response) {
        if (response == null) {
            return CoreWebVitals.unavailable("PageSpeed Insights không trả về dữ liệu.");
        }
        Integer labScore = labScore(response);

        // 1 và 2: dữ liệu người dùng thật
        Map<String, Object> page = map(response.get("loadingExperience"));
        if (hasFieldMetrics(page)) {
            Source source = Boolean.TRUE.equals(page.get("origin_fallback")) ? Source.FIELD_ORIGIN : Source.FIELD_URL;
            return fromField(page, source, labScore);
        }
        Map<String, Object> origin = map(response.get("originLoadingExperience"));
        if (hasFieldMetrics(origin)) {
            return fromField(origin, Source.FIELD_ORIGIN, labScore);
        }

        // 3: đo mô phỏng Lighthouse
        Map<String, Object> lighthouse = map(response.get("lighthouseResult"));
        Map<String, Object> runtimeError = map(lighthouse.get("runtimeError"));
        if (runtimeError.get("code") != null) {
            return CoreWebVitals.unavailable("Lighthouse không đo được trang: " + runtimeError.get("code") + ".");
        }
        Map<String, Object> audits = map(lighthouse.get("audits"));
        Double lcp = number(map(audits.get("largest-contentful-paint")).get("numericValue"));
        Double cls = number(map(audits.get("cumulative-layout-shift")).get("numericValue"));
        if (lcp == null && cls == null) {
            return CoreWebVitals.unavailable("PageSpeed Insights không có dữ liệu tốc độ cho trang này.");
        }
        return new CoreWebVitals(true, Source.LAB,
                lcp == null ? null : Math.round(lcp), null, cls, labScore, null);
    }

    private static CoreWebVitals fromField(Map<String, Object> experience, Source source, Integer labScore) {
        Map<String, Object> metrics = map(experience.get("metrics"));
        Double lcp = percentile(metrics, "LARGEST_CONTENTFUL_PAINT_MS");
        Double inp = percentile(metrics, "INTERACTION_TO_NEXT_PAINT");
        Double clsRaw = percentile(metrics, "CUMULATIVE_LAYOUT_SHIFT_SCORE");
        // Trong dữ liệu CrUX, CLS được nhân 100 để thành số nguyên (VD: 5 nghĩa là 0,05)
        Double cls = clsRaw == null ? null : clsRaw / 100.0;
        return new CoreWebVitals(true, source,
                lcp == null ? null : Math.round(lcp),
                inp == null ? null : Math.round(inp),
                cls, labScore, null);
    }

    private static boolean hasFieldMetrics(Map<String, Object> experience) {
        Map<String, Object> metrics = map(experience.get("metrics"));
        return metrics.containsKey("LARGEST_CONTENTFUL_PAINT_MS")
                || metrics.containsKey("CUMULATIVE_LAYOUT_SHIFT_SCORE")
                || metrics.containsKey("INTERACTION_TO_NEXT_PAINT");
    }

    private static Double percentile(Map<String, Object> metrics, String key) {
        return number(map(metrics.get(key)).get("percentile"));
    }

    private static Integer labScore(Map<String, Object> response) {
        Map<String, Object> categories = map(map(response.get("lighthouseResult")).get("categories"));
        Double score = number(map(categories.get("performance")).get("score"));
        return score == null ? null : (int) Math.round(score * 100);
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> map(Object value) {
        return value instanceof Map<?, ?> m ? (Map<String, Object>) m : Map.of();
    }

    private static Double number(Object value) {
        return value instanceof Number n ? n.doubleValue() : null;
    }
}

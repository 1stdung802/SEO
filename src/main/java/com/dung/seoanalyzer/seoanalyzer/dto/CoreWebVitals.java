package com.dung.seoanalyzer.seoanalyzer.dto;

/**
 * Chỉ số Core Web Vitals của một trang, lấy từ Google PageSpeed Insights.
 *
 * @param available         false nếu không đo được (khi đó các chỉ số đều null)
 * @param source            nguồn dữ liệu, xem {@link Source}
 * @param lcpMs             Largest Contentful Paint (mili giây), tốc độ hiện nội dung chính
 * @param inpMs             Interaction to Next Paint (mili giây), độ phản hồi khi tương tác;
 *                          chỉ có trong dữ liệu người dùng thật
 * @param cls               Cumulative Layout Shift, mức độ bố cục bị xô lệch (không đơn vị)
 * @param labScore          điểm hiệu năng Lighthouse 0–100 (tham khảo)
 * @param unavailableReason lý do không đo được, nếu có
 */
public record CoreWebVitals(
        boolean available,
        Source source,
        Long lcpMs,
        Long inpMs,
        Double cls,
        Integer labScore,
        String unavailableReason
) {
    public enum Source {
        /** Dữ liệu người dùng thật (CrUX) của chính URL này, 28 ngày gần nhất. */
        FIELD_URL("dữ liệu người dùng thật của trang này (28 ngày gần nhất)"),
        /** Trang không đủ lượt truy cập: dùng dữ liệu người dùng thật của cả tên miền. */
        FIELD_ORIGIN("dữ liệu người dùng thật của toàn tên miền (trang này chưa đủ lượt truy cập)"),
        /** Không có dữ liệu người dùng thật: dùng kết quả đo mô phỏng của Lighthouse. */
        LAB("đo mô phỏng bằng Lighthouse (trang chưa có dữ liệu người dùng thật)");

        private final String description;

        Source(String description) {
            this.description = description;
        }

        public String description() {
            return description;
        }
    }

    public static CoreWebVitals unavailable(String reason) {
        return new CoreWebVitals(false, null, null, null, null, null, reason);
    }
}

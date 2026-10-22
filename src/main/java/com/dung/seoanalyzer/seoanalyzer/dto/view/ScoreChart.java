package com.dung.seoanalyzer.seoanalyzer.dto.view;

import java.util.List;

/**
 * Dữ liệu đã tính sẵn toạ độ để vẽ biểu đồ đường "điểm SEO theo thời gian" bằng SVG.
 * Toàn bộ phép tính toạ độ làm trên server, template chỉ việc vẽ.
 *
 * @param polyline   chuỗi toạ độ cho thẻ polyline, VD "40,60 120,40"
 * @param gridLines  các đường kẻ ngang (mốc 0, 25, 50, 75, 100)
 * @param points     các điểm dữ liệu, cũ nhất bên trái
 * @param firstLabel nhãn thời gian của điểm đầu
 * @param lastLabel  nhãn thời gian của điểm cuối
 */
public record ScoreChart(
        int width,
        int height,
        String polyline,
        List<GridLine> gridLines,
        List<Point> points,
        String firstLabel,
        String lastLabel,
        int plotLeft,
        int plotRight,
        int labelY
) {
    public record GridLine(int value, double y) {}

    public record Point(Long id, double x, double y, int score, String label) {}
}

package com.dung.seoanalyzer.seoanalyzer.service;

import com.dung.seoanalyzer.seoanalyzer.dto.view.CriterionRow;
import com.dung.seoanalyzer.seoanalyzer.dto.view.HistoryRow;
import com.dung.seoanalyzer.seoanalyzer.dto.view.ResultView;
import com.dung.seoanalyzer.seoanalyzer.dto.view.ScoreChart;
import com.dung.seoanalyzer.seoanalyzer.entity.Analysis;
import com.dung.seoanalyzer.seoanalyzer.entity.AnalysisCriterion;
import com.dung.seoanalyzer.seoanalyzer.repository.AnalysisRepository;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.text.Normalizer;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.NoSuchElementException;

/**
 * Chuẩn bị dữ liệu hiển thị cho các trang giao diện (Thymeleaf).
 * Mọi việc định dạng và tính toán nằm ở đây để template chỉ việc hiển thị.
 */
@Service
public class ReportViewService {

    private static final DateTimeFormatter TIME_FORMAT = DateTimeFormatter.ofPattern("HH:mm, dd/MM/yyyy");
    private static final DateTimeFormatter SHORT_FORMAT = DateTimeFormatter.ofPattern("dd/MM HH:mm");

    /** Ngưỡng ký tự mô phỏng chỗ Google cắt title và meta description (gần đúng). */
    static final int SERP_TITLE_LIMIT = 60;
    static final int SERP_META_LIMIT = 160;

    /** Bán kính vòng đồng hồ điểm (khớp với r trong template). */
    private static final double GAUGE_RADIUS = 52;

    private final AnalysisRepository analysisRepository;

    public ReportViewService(AnalysisRepository analysisRepository) {
        this.analysisRepository = analysisRepository;
    }

    // ------------------------------------------------------------------ Trang kết quả

    public ResultView result(Long id) {
        Analysis a = analysisRepository.findWithCriteriaById(id)
                .orElseThrow(() -> new NoSuchElementException("Không tìm thấy lần phân tích số " + id + "."));

        List<CriterionRow> rows = a.getCriteria().stream().map(ReportViewService::toRow).toList();
        int passed = (int) rows.stream().filter(r -> r.status().equals("pass")).count();
        int partial = (int) rows.stream().filter(r -> r.status().equals("partial")).count();
        int failed = rows.size() - passed - partial;

        String title = nfc(a.getTitle());
        String meta = nfc(a.getMetaDescription());
        int titleLength = length(title);
        int metaLength = length(meta);

        return new ResultView(
                a.getId(), a.getUrl(), format(a.getAnalyzedAt(), TIME_FORMAT),
                a.getTotalScore(), band(a.getTotalScore()), bandLabel(a.getTotalScore()),
                gaugeDash(a.getTotalScore()),
                serpUrl(a.getUrl()),
                title.isEmpty() ? "(Trang chưa có thẻ title)" : cut(title, SERP_TITLE_LIMIT),
                titleLength > SERP_TITLE_LIMIT, titleLength,
                cut(meta, SERP_META_LIMIT), metaLength > SERP_META_LIMIT,
                a.getMetaDescription() != null, metaLength,
                passed, partial, failed, rows,
                a.getAiSuggestion(), format(a.getAiGeneratedAt(), TIME_FORMAT));
    }

    // ------------------------------------------------------------------ Lịch sử

    public List<HistoryRow> recent(int limit) {
        return analysisRepository.findTop20ByOrderByAnalyzedAtDesc().stream()
                .limit(limit)
                .map(ReportViewService::toHistoryRow)
                .toList();
    }

    /** Lịch sử của một URL; url trống thì trả về các lần phân tích gần nhất. */
    public List<HistoryRow> history(String rawUrl) {
        List<Analysis> analyses = (rawUrl == null || rawUrl.isBlank())
                ? analysisRepository.findTop20ByOrderByAnalyzedAtDesc()
                : analysisRepository.findTop20ByUrlOrderByAnalyzedAtDesc(AnalysisService.normalizeUrl(rawUrl));
        return analyses.stream().map(ReportViewService::toHistoryRow).toList();
    }

    /** Biểu đồ điểm theo thời gian của một URL; null nếu chưa đủ 2 lần phân tích. */
    public ScoreChart chartFor(String url) {
        List<Analysis> newestFirst = analysisRepository.findTop20ByUrlOrderByAnalyzedAtDesc(url);
        if (newestFirst.size() < 2) {
            return null;
        }
        List<Analysis> data = new ArrayList<>(newestFirst);
        Collections.reverse(data);
        return buildChart(data);
    }

    /** Như chartFor, nhưng nhận URL người dùng gõ và tự chuẩn hoá trước. */
    public ScoreChart chartForInput(String rawUrl) {
        return chartFor(AnalysisService.normalizeUrl(rawUrl));
    }

    // ------------------------------------------------------------------ Tính toán

    static ScoreChart buildChart(List<Analysis> oldestFirst) {
        final int width = 640, height = 220;
        final int left = 40, right = 20, top = 16, bottom = 36;
        final int plotW = width - left - right;
        final int plotH = height - top - bottom;

        List<ScoreChart.GridLine> grid = new ArrayList<>();
        for (int v = 0; v <= 100; v += 25) {
            grid.add(new ScoreChart.GridLine(v, round1(top + (100 - v) / 100.0 * plotH)));
        }

        int n = oldestFirst.size();
        List<ScoreChart.Point> points = new ArrayList<>();
        StringBuilder polyline = new StringBuilder();
        for (int i = 0; i < n; i++) {
            Analysis a = oldestFirst.get(i);
            double x = round1(left + (n == 1 ? plotW / 2.0 : i * (double) plotW / (n - 1)));
            double y = round1(top + (100 - a.getTotalScore()) / 100.0 * plotH);
            points.add(new ScoreChart.Point(a.getId(), x, y, a.getTotalScore(),
                    format(a.getAnalyzedAt(), SHORT_FORMAT)));
            if (i > 0) polyline.append(' ');
            polyline.append(x).append(',').append(y);
        }

        return new ScoreChart(width, height, polyline.toString(), grid, points,
                points.get(0).label(), points.get(n - 1).label(),
                left, width - right, height - 12);
    }

    static CriterionRow toRow(AnalysisCriterion c) {
        String status, label, icon;
        if (c.getScore() >= 1.0) {
            status = "pass"; label = "Đạt"; icon = "✓";
        } else if (c.getScore() > 0) {
            status = "partial"; label = "Đạt một phần"; icon = "!";
        } else {
            status = "fail"; label = "Chưa đạt"; icon = "✕";
        }
        String earned = formatNumber(c.getWeight() * c.getScore()) + " / " + c.getWeight();
        return new CriterionRow(c.getName(), c.getWeight(), earned, status, label, icon,
                c.getMessage(), c.getSource());
    }

    private static HistoryRow toHistoryRow(Analysis a) {
        return new HistoryRow(a.getId(), a.getUrl(),
                a.getTitle() == null || a.getTitle().isBlank() ? "(không có title)" : a.getTitle(),
                format(a.getAnalyzedAt(), TIME_FORMAT),
                a.getTotalScore(), band(a.getTotalScore()), bandLabel(a.getTotalScore()));
    }

    static String band(int score) {
        if (score >= 80) return "good";
        if (score >= 50) return "fair";
        return "poor";
    }

    static String bandLabel(int score) {
        if (score >= 80) return "Tốt";
        if (score >= 50) return "Cần cải thiện";
        return "Kém";
    }

    /** Độ dài nét vẽ của vòng đồng hồ, dạng "phần-đã-đạt phần-còn-lại". */
    static String gaugeDash(int score) {
        double circumference = 2 * Math.PI * GAUGE_RADIUS;
        double filled = Math.max(0, Math.min(100, score)) / 100.0 * circumference;
        return String.format(Locale.ROOT, "%.1f %.1f", filled, circumference - filled);
    }

    /** Hiển thị URL kiểu Google: "vnexpress.net › the-thao › bong-da". */
    static String serpUrl(String url) {
        try {
            URI uri = new URI(url);
            String host = uri.getHost() == null ? url : uri.getHost().replaceFirst("^www\\.", "");
            String path = uri.getPath() == null ? "" : uri.getPath();
            StringBuilder sb = new StringBuilder(host);
            for (String segment : path.split("/")) {
                if (!segment.isBlank()) sb.append(" › ").append(segment);
            }
            return sb.toString();
        } catch (Exception e) {
            return url;
        }
    }

    /** Cắt chuỗi tại giới hạn ký tự, lùi về khoảng trắng gần nhất rồi thêm "…" như Google. */
    static String cut(String text, int limit) {
        if (text.isEmpty() || length(text) <= limit) {
            return text;
        }
        int end = text.offsetByCodePoints(0, limit);
        int space = text.lastIndexOf(' ', end);
        if (space > end - 15 && space > 0) {
            end = space;
        }
        return text.substring(0, end).strip() + " …";
    }

    static int length(String text) {
        return text.codePointCount(0, text.length());
    }

    private static String nfc(String text) {
        return text == null ? "" : Normalizer.normalize(text.strip(), Normalizer.Form.NFC);
    }

    private static String format(LocalDateTime time, DateTimeFormatter formatter) {
        return time == null ? null : time.format(formatter);
    }

    private static String formatNumber(double value) {
        if (value == Math.rint(value)) {
            return String.valueOf((long) value);
        }
        return String.format(Locale.ROOT, "%.1f", value).replace('.', ',');
    }

    private static double round1(double v) {
        return Math.round(v * 10) / 10.0;
    }
}

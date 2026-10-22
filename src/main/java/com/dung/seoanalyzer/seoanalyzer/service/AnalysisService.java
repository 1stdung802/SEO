package com.dung.seoanalyzer.seoanalyzer.service;

import com.dung.seoanalyzer.seoanalyzer.dto.AnalysisDetail;
import com.dung.seoanalyzer.seoanalyzer.dto.AnalysisSummary;
import com.dung.seoanalyzer.seoanalyzer.dto.CoreWebVitals;
import com.dung.seoanalyzer.seoanalyzer.dto.CriterionResult;
import com.dung.seoanalyzer.seoanalyzer.dto.SeoPageData;
import com.dung.seoanalyzer.seoanalyzer.dto.SeoReport;
import com.dung.seoanalyzer.seoanalyzer.entity.Analysis;
import com.dung.seoanalyzer.seoanalyzer.entity.AnalysisCriterion;
import com.dung.seoanalyzer.seoanalyzer.repository.AnalysisRepository;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.concurrent.CompletableFuture;

/**
 * Điều phối toàn bộ quy trình: chuẩn hoá URL → thu thập → chấm điểm → lưu lịch sử.
 * <p>
 * Lưu ý thiết kế:
 * <ul>
 *   <li>Bước crawl và đo tốc độ KHÔNG nằm trong transaction, để không giữ kết nối
 *       database trong lúc chờ mạng. Chỉ thao tác save mới mở transaction.</li>
 *   <li>Đo tốc độ bằng PageSpeed Insights (10–40 giây) chạy SONG SONG với crawl,
 *       nên tổng thời gian bằng bước chậm nhất chứ không cộng dồn.</li>
 * </ul>
 */
@Service
public class AnalysisService {

    private final CrawlerService crawlerService;
    private final PageSpeedClient pageSpeedClient;
    private final SeoScoringService scoringService;
    private final AnalysisRepository analysisRepository;

    public AnalysisService(CrawlerService crawlerService,
                           PageSpeedClient pageSpeedClient,
                           SeoScoringService scoringService,
                           AnalysisRepository analysisRepository) {
        this.crawlerService = crawlerService;
        this.pageSpeedClient = pageSpeedClient;
        this.scoringService = scoringService;
        this.analysisRepository = analysisRepository;
    }

    /** Phân tích một URL, lưu kết quả vào database và trả về chi tiết. */
    public AnalysisDetail analyzeAndSave(String rawUrl) throws IOException {
        String url = normalizeUrl(rawUrl);

        // Bắt đầu đo tốc độ ở luồng riêng, trong lúc luồng chính crawl trang
        CompletableFuture<CoreWebVitals> vitalsFuture =
                CompletableFuture.supplyAsync(() -> pageSpeedClient.measure(url));

        SeoPageData data = crawlerService.crawl(url);
        CoreWebVitals vitals = vitalsFuture.join(); // measure() không ném lỗi, luôn có kết quả
        data = data.withCoreWebVitals(vitals);

        SeoReport report = scoringService.score(data);

        Analysis analysis = new Analysis(url,
                truncate(data.title(), Analysis.MAX_TITLE_LENGTH),
                truncate(data.metaDescription(), Analysis.MAX_META_LENGTH),
                report.analyzedAt(), report.totalScore());
        report.results().forEach(result -> analysis.addCriterion(AnalysisCriterion.from(result)));

        Analysis saved = analysisRepository.save(analysis);
        return new AnalysisDetail(saved.getId(), saved.getUrl(), saved.getTitle(),
                saved.getAnalyzedAt(), saved.getTotalScore(), report.results(), data);
    }

    /** Lịch sử phân tích: của một URL cụ thể, hoặc của mọi URL nếu không truyền url. */
    public List<AnalysisSummary> history(String rawUrl) {
        List<Analysis> analyses = (rawUrl == null || rawUrl.isBlank())
                ? analysisRepository.findTop20ByOrderByAnalyzedAtDesc()
                : analysisRepository.findTop20ByUrlOrderByAnalyzedAtDesc(normalizeUrl(rawUrl));

        return analyses.stream()
                .map(a -> new AnalysisSummary(a.getId(), a.getUrl(), a.getTitle(),
                        a.getAnalyzedAt(), a.getTotalScore()))
                .toList();
    }

    /** Xem lại chi tiết một lần phân tích đã lưu. */
    public AnalysisDetail detail(Long id) {
        Analysis analysis = analysisRepository.findWithCriteriaById(id)
                .orElseThrow(() -> new NoSuchElementException("Không tìm thấy lần phân tích có id = " + id));

        List<CriterionResult> results = analysis.getCriteria().stream()
                .map(AnalysisCriterion::toResult)
                .toList();
        return new AnalysisDetail(analysis.getId(), analysis.getUrl(), analysis.getTitle(),
                analysis.getAnalyzedAt(), analysis.getTotalScore(), results, null);
    }

    /**
     * Chuẩn hoá URL để cùng một trang luôn được lưu cùng một cách viết:
     * tự thêm https:// nếu thiếu, bỏ dấu "/" thừa ở trang chủ (example.com/ → example.com).
     */
    static String normalizeUrl(String rawUrl) {
        if (rawUrl == null || rawUrl.isBlank()) {
            throw new IllegalArgumentException("Vui lòng nhập URL cần phân tích.");
        }
        String url = rawUrl.strip();
        if (!url.contains("://")) {
            url = "https://" + url;
        }

        URI uri;
        try {
            uri = new URI(url);
        } catch (URISyntaxException e) {
            throw new IllegalArgumentException("URL không hợp lệ: " + rawUrl);
        }
        String scheme = uri.getScheme() == null ? "" : uri.getScheme().toLowerCase();
        if (!scheme.equals("http") && !scheme.equals("https")) {
            throw new IllegalArgumentException("Chỉ hỗ trợ URL bắt đầu bằng http:// hoặc https://");
        }
        if (uri.getHost() == null) {
            throw new IllegalArgumentException("URL không có tên miền hợp lệ: " + rawUrl);
        }

        String path = uri.getRawPath();
        if ("/".equals(path) && uri.getRawQuery() == null && uri.getRawFragment() == null) {
            url = url.substring(0, url.length() - 1);
        }
        if (url.length() > Analysis.MAX_URL_LENGTH) {
            throw new IllegalArgumentException("URL quá dài (tối đa " + Analysis.MAX_URL_LENGTH + " ký tự).");
        }
        return url;
    }

    private static String truncate(String value, int max) {
        if (value == null || value.length() <= max) {
            return value;
        }
        return value.substring(0, max - 3) + "...";
    }
}

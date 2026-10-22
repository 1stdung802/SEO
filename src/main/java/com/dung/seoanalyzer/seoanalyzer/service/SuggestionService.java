package com.dung.seoanalyzer.seoanalyzer.service;

import com.dung.seoanalyzer.seoanalyzer.dto.SuggestionResponse;
import com.dung.seoanalyzer.seoanalyzer.entity.Analysis;
import com.dung.seoanalyzer.seoanalyzer.entity.AnalysisCriterion;
import com.dung.seoanalyzer.seoanalyzer.repository.AnalysisRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.NoSuchElementException;

/**
 * Sinh gợi ý cải thiện SEO bằng AI cho một lần phân tích đã lưu.
 * <p>
 * Ba nguyên tắc thiết kế:
 * <ol>
 *   <li>Chỉ gọi AI khi có tiêu chí chưa đạt, để tiết kiệm hạn mức miễn phí.</li>
 *   <li>Lưu gợi ý vào database: lần xem sau trả về ngay, không gọi lại AI.</li>
 *   <li>Chỉ gửi cho AI dữ liệu đã thu thập, yêu cầu không bịa thông tin.</li>
 * </ol>
 */
@Service
public class SuggestionService {

    static final String SYSTEM_INSTRUCTION = """
            Bạn là chuyên gia SEO on-page. Trả lời bằng tiếng Việt, ngắn gọn, cụ thể, \
            người đọc có thể làm theo ngay. Chỉ dựa trên dữ liệu được cung cấp, \
            không suy đoán hay bịa thông tin về nội dung trang.""";

    private final AnalysisRepository analysisRepository;
    private final GeminiClient geminiClient;

    public SuggestionService(AnalysisRepository analysisRepository, GeminiClient geminiClient) {
        this.analysisRepository = analysisRepository;
        this.geminiClient = geminiClient;
    }

    /** Lấy gợi ý đã lưu, hoặc sinh mới bằng AI nếu chưa có. */
    public SuggestionResponse getOrCreate(Long analysisId) {
        Analysis analysis = analysisRepository.findWithCriteriaById(analysisId)
                .orElseThrow(() -> new NoSuchElementException(
                        "Không tìm thấy lần phân tích có id = " + analysisId));

        if (analysis.getAiSuggestion() != null) {
            return toResponse(analysis, true);
        }

        List<AnalysisCriterion> failed = analysis.getCriteria().stream()
                // Bỏ qua tiêu chí không tính điểm (trọng số 0, VD: không đo được tốc độ)
                .filter(c -> c.getWeight() > 0 && c.getScore() < 1.0)
                .sorted(Comparator.comparingInt(AnalysisCriterion::getWeight).reversed())
                .toList();

        String suggestion = failed.isEmpty()
                ? "Trang đã đạt tất cả tiêu chí SEO on-page đang kiểm tra, chưa cần chỉnh sửa thêm."
                : geminiClient.generate(SYSTEM_INSTRUCTION, buildPrompt(analysis, failed));

        analysis.attachAiSuggestion(suggestion, LocalDateTime.now());
        Analysis saved = analysisRepository.save(analysis);
        return toResponse(saved, false);
    }

    /** Dựng câu lệnh gửi AI từ dữ liệu phân tích. */
    static String buildPrompt(Analysis analysis, List<AnalysisCriterion> failed) {
        StringBuilder sb = new StringBuilder();
        sb.append("Trang: ").append(analysis.getUrl()).append('\n');
        sb.append("Title hiện tại: ").append(valueOrNone(analysis.getTitle())).append('\n');
        sb.append("Meta description hiện tại: ").append(valueOrNone(analysis.getMetaDescription())).append('\n');
        sb.append("Điểm SEO on-page: ").append(analysis.getTotalScore()).append("/100\n\n");

        sb.append("Các tiêu chí chưa đạt (trọng số cao trước):\n");
        for (AnalysisCriterion c : failed) {
            sb.append("- ").append(c.getName())
              .append(" (trọng số ").append(c.getWeight())
              .append(", đạt ").append(Math.round(c.getScore() * 100)).append("%): ")
              .append(c.getMessage()).append('\n');
        }

        sb.append("""

                Yêu cầu:
                1. Với mỗi tiêu chí chưa đạt, đưa ra 1-2 hành động khắc phục cụ thể.
                2. Nếu title hoặc meta description chưa đạt, đề xuất một phiên bản viết lại \
                đúng ngưỡng (title 30-60 ký tự, meta description 70-160 ký tự), \
                giữ đúng ngôn ngữ của trang.
                3. Trình bày theo thứ tự ưu tiên như danh sách trên, dạng gạch đầu dòng, \
                không dùng ký hiệu ** hay #, tối đa 250 từ.""");
        return sb.toString();
    }

    private SuggestionResponse toResponse(Analysis a, boolean cached) {
        return new SuggestionResponse(a.getId(), a.getUrl(), a.getTotalScore(),
                a.getAiSuggestion(), a.getAiGeneratedAt(), geminiClient.getModel(), cached);
    }

    private static String valueOrNone(String value) {
        return value == null || value.isBlank() ? "(không có)" : value;
    }
}

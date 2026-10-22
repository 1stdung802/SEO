package com.dung.seoanalyzer.seoanalyzer.controller;

import com.dung.seoanalyzer.seoanalyzer.dto.AnalysisDetail;
import com.dung.seoanalyzer.seoanalyzer.dto.AnalysisSummary;
import com.dung.seoanalyzer.seoanalyzer.dto.SuggestionResponse;
import com.dung.seoanalyzer.seoanalyzer.service.AiServiceException;
import com.dung.seoanalyzer.seoanalyzer.service.AnalysisService;
import com.dung.seoanalyzer.seoanalyzer.service.SuggestionService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;

/**
 * API phân tích SEO và xem lịch sử, trả về JSON.
 * <ul>
 *   <li>GET /api/analyze?url=...   phân tích và lưu kết quả</li>
 *   <li>GET /api/history           20 lần phân tích gần nhất</li>
 *   <li>GET /api/history?url=...   lịch sử của một URL</li>
 *   <li>GET /api/history/{id}      chi tiết một lần phân tích</li>
 *   <li>GET /api/history/{id}/suggestions  gợi ý cải thiện bằng AI
 *       (lần đầu gọi AI và lưu lại, các lần sau trả về bản đã lưu)</li>
 * </ul>
 */
@RestController
@RequestMapping("/api")
public class AnalysisApiController {

    private final AnalysisService analysisService;
    private final SuggestionService suggestionService;

    public AnalysisApiController(AnalysisService analysisService, SuggestionService suggestionService) {
        this.analysisService = analysisService;
        this.suggestionService = suggestionService;
    }

    @GetMapping("/analyze")
    public AnalysisDetail analyze(@RequestParam String url) throws IOException {
        return analysisService.analyzeAndSave(url);
    }

    @GetMapping("/history")
    public List<AnalysisSummary> history(@RequestParam(required = false) String url) {
        return analysisService.history(url);
    }

    @GetMapping("/history/{id}")
    public AnalysisDetail detail(@PathVariable Long id) {
        return analysisService.detail(id);
    }

    @GetMapping("/history/{id}/suggestions")
    public SuggestionResponse suggestions(@PathVariable Long id) {
        return suggestionService.getOrCreate(id);
    }

    // ---- Xử lý lỗi: trả về thông báo dễ hiểu thay cho trang Whitelabel Error ----

    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<Map<String, String>> handleMissingParam(MissingServletRequestParameterException e) {
        return error(HttpStatus.BAD_REQUEST, "Thiếu tham số '" + e.getParameterName()
                + "'. Ví dụ: /api/analyze?url=https://example.com");
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, String>> handleBadInput(IllegalArgumentException e) {
        return error(HttpStatus.BAD_REQUEST, e.getMessage());
    }

    @ExceptionHandler(NoSuchElementException.class)
    public ResponseEntity<Map<String, String>> handleNotFound(NoSuchElementException e) {
        return error(HttpStatus.NOT_FOUND, e.getMessage());
    }

    @ExceptionHandler(IOException.class)
    public ResponseEntity<Map<String, String>> handleCrawlError(IOException e) {
        return error(HttpStatus.BAD_GATEWAY, "Không truy cập được trang cần phân tích: " + e.getMessage());
    }

    @ExceptionHandler(AiServiceException.class)
    public ResponseEntity<Map<String, String>> handleAiError(AiServiceException e) {
        HttpStatus status = switch (e.getHttpStatus()) {
            case 429 -> HttpStatus.TOO_MANY_REQUESTS;
            case 503 -> HttpStatus.SERVICE_UNAVAILABLE;
            case 504 -> HttpStatus.GATEWAY_TIMEOUT;
            default -> HttpStatus.BAD_GATEWAY;
        };
        return error(status, e.getMessage());
    }

    private ResponseEntity<Map<String, String>> error(HttpStatus status, String message) {
        return ResponseEntity.status(status).body(Map.of("error", message == null ? status.toString() : message));
    }
}

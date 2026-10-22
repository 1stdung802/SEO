package com.dung.seoanalyzer.seoanalyzer.controller;

import com.dung.seoanalyzer.seoanalyzer.dto.AnalysisDetail;
import com.dung.seoanalyzer.seoanalyzer.dto.view.ResultView;
import com.dung.seoanalyzer.seoanalyzer.service.AiServiceException;
import com.dung.seoanalyzer.seoanalyzer.service.AnalysisService;
import com.dung.seoanalyzer.seoanalyzer.service.ReportViewService;
import com.dung.seoanalyzer.seoanalyzer.service.SuggestionService;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.io.IOException;
import java.util.NoSuchElementException;

/**
 * Các trang giao diện, render phía server bằng Thymeleaf.
 * <ul>
 *   <li>GET  /                           trang chủ: nhập URL + các lần phân tích gần đây</li>
 *   <li>POST /analyze                    phân tích rồi chuyển sang trang kết quả</li>
 *   <li>GET  /analysis/{id}              trang kết quả</li>
 *   <li>POST /analysis/{id}/suggestions  tạo gợi ý AI rồi quay lại trang kết quả</li>
 *   <li>GET  /history                    trang lịch sử (lọc theo URL)</li>
 * </ul>
 * Các thao tác POST dùng mô hình Post/Redirect/Get: xử lý xong thì chuyển hướng,
 * nên bấm tải lại trang không gửi lại yêu cầu phân tích lần nữa.
 */
@Controller
public class PageController {

    private static final int RECENT_LIMIT = 8;

    private final AnalysisService analysisService;
    private final SuggestionService suggestionService;
    private final ReportViewService viewService;

    public PageController(AnalysisService analysisService,
                          SuggestionService suggestionService,
                          ReportViewService viewService) {
        this.analysisService = analysisService;
        this.suggestionService = suggestionService;
        this.viewService = viewService;
    }

    @GetMapping("/")
    public String index(Model model) {
        model.addAttribute("recent", viewService.recent(RECENT_LIMIT));
        return "index";
    }

    @PostMapping("/analyze")
    public String analyze(@RequestParam(required = false) String url, Model model) {
        String error;
        try {
            AnalysisDetail detail = analysisService.analyzeAndSave(url);
            return "redirect:/analysis/" + detail.id();
        } catch (IllegalArgumentException e) {
            error = e.getMessage();
        } catch (IOException e) {
            error = "Không mở được trang này: " + e.getMessage()
                    + ". Kiểm tra lại địa chỉ, hoặc trang có thể đang chặn công cụ thu thập dữ liệu.";
        }
        model.addAttribute("error", error);
        model.addAttribute("url", url);
        model.addAttribute("recent", viewService.recent(RECENT_LIMIT));
        return "index";
    }

    @GetMapping("/analysis/{id}")
    public String result(@PathVariable Long id, Model model) {
        ResultView view = viewService.result(id);
        model.addAttribute("r", view);
        model.addAttribute("chart", viewService.chartFor(view.url()));
        return "result";
    }

    @PostMapping("/analysis/{id}/suggestions")
    public String suggestions(@PathVariable Long id, RedirectAttributes redirect) {
        try {
            suggestionService.getOrCreate(id);
        } catch (AiServiceException e) {
            redirect.addFlashAttribute("aiError", e.getMessage());
        }
        return "redirect:/analysis/" + id + "#ai";
    }

    @GetMapping("/history")
    public String history(@RequestParam(required = false) String url, Model model) {
        model.addAttribute("url", url);
        try {
            model.addAttribute("rows", viewService.history(url));
            if (url != null && !url.isBlank()) {
                model.addAttribute("chart", viewService.chartForInput(url));
            }
        } catch (IllegalArgumentException e) {
            model.addAttribute("error", e.getMessage());
            model.addAttribute("rows", viewService.history(null));
        }
        return "history";
    }

    /** Trang báo lỗi khi mở một lần phân tích không tồn tại. */
    @ExceptionHandler(NoSuchElementException.class)
    public ModelAndView notFound(NoSuchElementException e) {
        ModelAndView mav = new ModelAndView("error");
        mav.setStatus(HttpStatus.NOT_FOUND);
        mav.addObject("status", 404);
        mav.addObject("message", e.getMessage());
        return mav;
    }
}

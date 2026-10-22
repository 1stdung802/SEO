package com.dung.seoanalyzer.seoanalyzer.entity;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Một lần phân tích SEO của một URL (bảng "analyses").
 * <p>
 * Quan hệ: 1 Analysis có nhiều AnalysisCriterion (kết quả từng tiêu chí).
 * Mỗi lần phân tích là một bản ghi riêng, nhờ đó so sánh được điểm SEO
 * của cùng một trang theo thời gian.
 */
@Entity
@Table(name = "analyses", indexes = @Index(name = "idx_analyses_url", columnList = "url"))
public class Analysis {

    /** Độ dài tối đa của URL: giới hạn để MySQL tạo được chỉ mục (index) trên cột url. */
    public static final int MAX_URL_LENGTH = 768;
    public static final int MAX_TITLE_LENGTH = 512;
    public static final int MAX_META_LENGTH = 1000;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = MAX_URL_LENGTH)
    private String url;

    @Column(length = MAX_TITLE_LENGTH)
    private String title;

    @Column(name = "analyzed_at", nullable = false)
    private LocalDateTime analyzedAt;

    @Column(name = "total_score", nullable = false)
    private int totalScore;

    /** Meta description tại thời điểm phân tích, làm ngữ cảnh để AI viết lại. */
    @Column(name = "meta_description", length = MAX_META_LENGTH)
    private String metaDescription;

    /** Gợi ý cải thiện do AI sinh ra; lưu lại để không phải gọi AI lần nữa. */
    @Column(name = "ai_suggestion", columnDefinition = "TEXT")
    private String aiSuggestion;

    @Column(name = "ai_generated_at")
    private LocalDateTime aiGeneratedAt;

    @OneToMany(mappedBy = "analysis", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("id ASC")
    private List<AnalysisCriterion> criteria = new ArrayList<>();

    /** Constructor rỗng bắt buộc cho JPA. */
    protected Analysis() {
    }

    public Analysis(String url, String title, String metaDescription,
                    LocalDateTime analyzedAt, int totalScore) {
        this.url = url;
        this.title = title;
        this.metaDescription = metaDescription;
        this.analyzedAt = analyzedAt;
        this.totalScore = totalScore;
    }

    /** Ghi nhận gợi ý AI vừa sinh ra cùng thời điểm sinh. */
    public void attachAiSuggestion(String suggestion, LocalDateTime generatedAt) {
        this.aiSuggestion = suggestion;
        this.aiGeneratedAt = generatedAt;
    }

    /** Thêm kết quả tiêu chí và gắn liên kết hai chiều. */
    public void addCriterion(AnalysisCriterion criterion) {
        criteria.add(criterion);
        criterion.setAnalysis(this);
    }

    public Long getId() {
        return id;
    }

    public String getUrl() {
        return url;
    }

    public String getTitle() {
        return title;
    }

    public LocalDateTime getAnalyzedAt() {
        return analyzedAt;
    }

    public int getTotalScore() {
        return totalScore;
    }

    public String getMetaDescription() {
        return metaDescription;
    }

    public String getAiSuggestion() {
        return aiSuggestion;
    }

    public LocalDateTime getAiGeneratedAt() {
        return aiGeneratedAt;
    }

    public List<AnalysisCriterion> getCriteria() {
        return Collections.unmodifiableList(criteria);
    }
}

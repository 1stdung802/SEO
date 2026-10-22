package com.dung.seoanalyzer.seoanalyzer.entity;

import com.dung.seoanalyzer.seoanalyzer.dto.CriterionResult;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

/**
 * Kết quả của một tiêu chí trong một lần phân tích (bảng "analysis_criteria").
 * Mỗi bản ghi thuộc về đúng một Analysis qua khoá ngoại analysis_id.
 */
@Entity
@Table(name = "analysis_criteria")
public class AnalysisCriterion {

    private static final int MAX_MESSAGE_LENGTH = 1000;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "analysis_id", nullable = false)
    private Analysis analysis;

    @Column(nullable = false, length = 50)
    private String code;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private int weight;

    @Column(nullable = false)
    private double score;

    @Column(length = MAX_MESSAGE_LENGTH)
    private String message;

    @Column
    private String source;

    /** Constructor rỗng bắt buộc cho JPA. */
    protected AnalysisCriterion() {
    }

    /** Chuyển từ kết quả chấm điểm (DTO) sang bản ghi lưu database. */
    public static AnalysisCriterion from(CriterionResult result) {
        AnalysisCriterion c = new AnalysisCriterion();
        c.code = result.code();
        c.name = result.name();
        c.weight = result.weight();
        c.score = result.score();
        c.message = truncate(result.message(), MAX_MESSAGE_LENGTH);
        c.source = truncate(result.source(), 255);
        return c;
    }

    /** Chuyển ngược lại thành DTO để trả về cho client. */
    public CriterionResult toResult() {
        return new CriterionResult(code, name, weight, score, message, source);
    }

    void setAnalysis(Analysis analysis) {
        this.analysis = analysis;
    }

    public Long getId() {
        return id;
    }

    public String getCode() {
        return code;
    }

    public String getName() {
        return name;
    }

    public int getWeight() {
        return weight;
    }

    public double getScore() {
        return score;
    }

    public String getMessage() {
        return message;
    }

    public String getSource() {
        return source;
    }

    /** Cắt chuỗi quá dài để không vượt độ dài cột (VD: thông báo chứa URL canonical rất dài). */
    private static String truncate(String value, int max) {
        if (value == null || value.length() <= max) {
            return value;
        }
        return value.substring(0, max - 3) + "...";
    }
}

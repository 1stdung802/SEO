package com.dung.seoanalyzer.seoanalyzer.repository;

import com.dung.seoanalyzer.seoanalyzer.entity.Analysis;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

/**
 * Truy vấn bảng analyses. Spring Data JPA tự sinh câu SQL từ tên phương thức,
 * không cần viết SQL thủ công.
 */
public interface AnalysisRepository extends JpaRepository<Analysis, Long> {

    /** 20 lần phân tích gần nhất của một URL, mới nhất trước. */
    List<Analysis> findTop20ByUrlOrderByAnalyzedAtDesc(String url);

    /** 20 lần phân tích gần nhất của mọi URL, mới nhất trước. */
    List<Analysis> findTop20ByOrderByAnalyzedAtDesc();

    /** Lấy một lần phân tích kèm luôn kết quả các tiêu chí trong cùng một truy vấn. */
    @EntityGraph(attributePaths = "criteria")
    Optional<Analysis> findWithCriteriaById(Long id);
}

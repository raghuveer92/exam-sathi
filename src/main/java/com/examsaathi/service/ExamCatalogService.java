package com.examsaathi.service;

import com.examsaathi.config.CacheNames;
import com.examsaathi.dto.response.ExamCatalogResponse;
import com.examsaathi.dto.response.ExamCategoryResponse;
import com.examsaathi.dto.response.ExamResponse;
import com.examsaathi.entity.Exam;
import com.examsaathi.repository.ExamRepository;
import com.examsaathi.repository.UserExamRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ExamCatalogService {

    private final ExamCategoryService categoryService;
    private final ExamRepository examRepository;
    private final UserExamRepository userExamRepository;
    private final UserMapper mapper;

    @Transactional(readOnly = true)
    @Cacheable(value = CacheNames.EXAM_CATALOG, key = "'catalog_' + (#userId != null ? #userId : 'anon')")
    public ExamCatalogResponse getCatalog(Long userId) {
        List<ExamCategoryResponse> categories = categoryService.getActiveCategories();
        List<ExamResponse> featured = examRepository.findByIsActiveTrueAndFeaturedTrueOrderByFeaturedOrderAscDisplayOrderAscNameAsc()
            .stream().map(e -> mapper.toExamResponse(e, false)).collect(Collectors.toList());
        List<ExamResponse> recommended = getRecommended(userId);
        return ExamCatalogResponse.builder()
            .categories(categories)
            .featuredExams(featured)
            .recommendedExams(recommended)
            .build();
    }

    @Transactional(readOnly = true)
    @Cacheable(value = CacheNames.EXAM_CATALOG, key = "'category_' + #categoryId")
    public List<ExamResponse> getExamsByCategory(Long categoryId) {
        return examRepository.findByIsActiveTrueAndCategoryIdOrderByDisplayOrderAscNameAsc(categoryId)
            .stream().map(e -> mapper.toExamResponse(e, false)).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    @Cacheable(value = CacheNames.EXAM_CATALOG, key = "'featured'")
    public List<ExamResponse> getFeatured() {
        return examRepository.findByIsActiveTrueAndFeaturedTrueOrderByFeaturedOrderAscDisplayOrderAscNameAsc()
            .stream().map(e -> mapper.toExamResponse(e, false)).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    @Cacheable(value = CacheNames.EXAM_CATALOG, key = "'search_' + (#query != null ? #query.trim().toLowerCase() : '')")
    public List<ExamResponse> search(String query) {
        if (query == null || query.trim().isEmpty()) {
            return examRepository.findByIsActiveTrueOrderByDisplayOrderAscNameAsc()
                .stream().map(e -> mapper.toExamResponse(e, false)).collect(Collectors.toList());
        }
        return examRepository.searchActive(query.trim())
            .stream().map(e -> mapper.toExamResponse(e, false)).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    @Cacheable(value = CacheNames.EXAM_CATALOG, key = "'recommended_' + (#userId != null ? #userId : 'anon')")
    public List<ExamResponse> getRecommended(Long userId) {
        Set<Long> enrolled = new HashSet<>();
        if (userId != null) {
            userExamRepository.findByUserIdOrderByCreatedAtAsc(userId)
                .forEach(ue -> enrolled.add(ue.getExam().getId()));
        }
        List<Exam> popular = examRepository.findByIsActiveTrueAndPopularTrueOrderByDisplayOrderAscNameAsc();
        if (popular.isEmpty()) {
            popular = examRepository.findByIsActiveTrueAndFeaturedTrueOrderByFeaturedOrderAscDisplayOrderAscNameAsc();
        }
        if (popular.isEmpty()) {
            popular = examRepository.findByIsActiveTrueOrderByDisplayOrderAscNameAsc();
        }
        return popular.stream()
            .filter(e -> !enrolled.contains(e.getId()))
            .limit(6)
            .map(e -> mapper.toExamResponse(e, false))
            .collect(Collectors.toList());
    }
}

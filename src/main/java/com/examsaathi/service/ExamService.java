package com.examsaathi.service;

import com.examsaathi.config.CacheKeyBuilder;
import com.examsaathi.config.CacheNames;
import com.examsaathi.dto.request.ExamRequest;
import com.examsaathi.dto.response.ExamResponse;
import com.examsaathi.entity.Exam;
import com.examsaathi.entity.ExamCategory;
import com.examsaathi.exception.ResourceNotFoundException;
import com.examsaathi.repository.DailyStudyLogRepository;
import com.examsaathi.repository.ExamRepository;
import com.examsaathi.repository.ExamSubjectGroupRepository;
import com.examsaathi.repository.ExamSubjectRepository;
import com.examsaathi.repository.QuestionRepository;
import com.examsaathi.repository.StudyProgressRepository;
import com.examsaathi.repository.TestAttemptAnswerRepository;
import com.examsaathi.repository.UserExamRepository;
import com.examsaathi.repository.UserExamSubjectSelectionRepository;
import com.examsaathi.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ExamService {

    private final ExamRepository examRepository;
    private final ExamSubjectGroupRepository examSubjectGroupRepository;
    private final ExamSubjectRepository examSubjectRepository;
    private final ExamCategoryService categoryService;
    private final UserMapper mapper;
    private final UserRepository userRepository;
    private final QuestionRepository questionRepository;
    private final TestAttemptAnswerRepository testAttemptAnswerRepository;
    private final DailyStudyLogRepository dailyStudyLogRepository;
    private final StudyProgressRepository studyProgressRepository;
    private final UserExamSubjectSelectionRepository userExamSubjectSelectionRepository;
    private final UserExamRepository userExamRepository;
    private final CacheEvictionService cacheEvictionService;

    @Transactional(readOnly = true)
    @Cacheable(value = CacheNames.EXAM_LIST, key = "'all'")
    public List<ExamResponse> getAllActiveExams() {
        return examRepository.findByIsActiveTrueOrderByDisplayOrderAscNameAsc()
            .stream().map(e -> mapper.toExamResponse(e, false)).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    @Cacheable(value = CacheNames.EXAMS, key = "T(com.examsaathi.config.CacheKeyBuilder).exam(#id)")
    public ExamResponse getExamById(Long id) {
        Exam exam = examRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Exam", id));
        return mapper.toExamResponse(exam, true);
    }

    @Transactional
    public ExamResponse createExam(ExamRequest request) {
        Exam exam = Exam.builder()
            .name(request.getName())
            .description(request.getDescription())
            .shortDescription(request.getShortDescription())
            .category(resolveCategory(request.getCategoryId()))
            .bannerUrl(request.getBannerUrl())
            .difficultyLevel(request.getDifficultyLevel())
            .featured(Boolean.TRUE.equals(request.getFeatured()))
            .popular(Boolean.TRUE.equals(request.getPopular()))
            .displayOrder(request.getDisplayOrder() != null ? request.getDisplayOrder() : 0)
            .featuredOrder(request.getFeaturedOrder() != null ? request.getFeaturedOrder() : 0)
            .code(request.getCode())
            .iconUrl(request.getIconUrl())
            .colorCode(request.getColorCode())
            .isActive(request.getIsActive())
            .build();
        ExamResponse response = mapper.toExamResponse(examRepository.save(exam), false);
        cacheEvictionService.evictCatalogData();
        return response;
    }

    @Transactional
    public ExamResponse updateExam(Long id, ExamRequest request) {
        Exam exam = examRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Exam", id));
        applyRequest(exam, request);
        ExamResponse response = mapper.toExamResponse(examRepository.save(exam), false);
        cacheEvictionService.evictCatalogData();
        return response;
    }

    private void applyRequest(Exam exam, ExamRequest request) {
        exam.setName(request.getName());
        exam.setDescription(request.getDescription());
        exam.setShortDescription(request.getShortDescription());
        exam.setCategory(resolveCategory(request.getCategoryId()));
        exam.setBannerUrl(request.getBannerUrl());
        exam.setDifficultyLevel(request.getDifficultyLevel());
        if (request.getFeatured() != null) exam.setFeatured(request.getFeatured());
        if (request.getPopular() != null) exam.setPopular(request.getPopular());
        if (request.getDisplayOrder() != null) exam.setDisplayOrder(request.getDisplayOrder());
        if (request.getFeaturedOrder() != null) exam.setFeaturedOrder(request.getFeaturedOrder());
        exam.setCode(request.getCode());
        exam.setIconUrl(request.getIconUrl());
        exam.setColorCode(request.getColorCode());
        if (request.getIsActive() != null) exam.setIsActive(request.getIsActive());
    }

    private ExamCategory resolveCategory(Long categoryId) {
        if (categoryId == null) return null;
        return categoryService.getEntity(categoryId);
    }

    @Transactional
    public void deleteExam(Long id) {
        Exam exam = examRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Exam", id));

        userRepository.clearSelectedExam(id);
        testAttemptAnswerRepository.deleteByQuestionExamId(id);
        questionRepository.deleteAll(questionRepository.findByExamId(id));
        dailyStudyLogRepository.deleteByExamId(id);
        studyProgressRepository.deleteByExamId(id);
        userExamSubjectSelectionRepository.deleteByExamId(id);
        userExamRepository.deleteByExamId(id);
        examSubjectGroupRepository.deleteAll(
            examSubjectGroupRepository.findByExamIdOrderByDisplayOrderAscIdAsc(id));
        examSubjectRepository.deleteByExamId(id);

        examRepository.delete(exam);
        cacheEvictionService.evictCatalogData();
    }
}

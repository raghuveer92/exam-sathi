package com.examsaathi.service;

import com.examsaathi.config.CacheNames;
import com.examsaathi.dto.request.TopicTestConfigRequest;
import com.examsaathi.dto.response.TopicTestConfigResponse;
import com.examsaathi.dto.sheet.SheetQuestion;
import com.examsaathi.entity.Exam;
import com.examsaathi.entity.ExamSubject;
import com.examsaathi.entity.Topic;
import com.examsaathi.entity.TopicTestConfig;
import com.examsaathi.exception.ResourceNotFoundException;
import com.examsaathi.repository.ExamSubjectRepository;
import com.examsaathi.repository.QuestionRepository;
import com.examsaathi.repository.TestAttemptAnswerRepository;
import com.examsaathi.repository.TestAttemptRepository;
import com.examsaathi.repository.TopicRepository;
import com.examsaathi.repository.TopicTestConfigRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
@Slf4j
public class TopicTestConfigService {

    private final TopicTestConfigRepository configRepository;
    private final TopicRepository topicRepository;
    private final QuestionRepository questionRepository;
    private final TestAttemptAnswerRepository testAttemptAnswerRepository;
    private final TestAttemptRepository testAttemptRepository;
    private final QuestionBankService questionBankService;
    private final CacheEvictionService cacheEvictionService;
    private final GoogleSheetsService googleSheetsService;
    private final ExamSubjectRepository examSubjectRepository;

    @Transactional(readOnly = true)
    @Cacheable(value = CacheNames.TOPIC_TEST_CONFIG, key = "'all'")
    public List<TopicTestConfigResponse> listAll() {
        return configRepository.findAll().stream().map(this::toResponse).toList();
    }

    @Transactional(readOnly = true)
    @Cacheable(value = CacheNames.TOPIC_TEST_CONFIG, key = "'topic_' + #topicId")
    public TopicTestConfigResponse getByTopicId(Long topicId) {
        return configRepository.findByTopicId(topicId)
            .map(this::toResponse)
            .orElseGet(() -> defaultResponse(topicId));
    }

    public TopicTestConfigResponse upsert(TopicTestConfigRequest request) {
        Topic topic = topicRepository.findById(request.getTopicId())
            .orElseThrow(() -> new ResourceNotFoundException("Topic", request.getTopicId()));

        TopicTestConfig config = configRepository.findByTopicId(request.getTopicId())
            .orElseGet(() -> TopicTestConfig.builder().topic(topic).build());

        config.setTopic(topic);
        config.setNumQuestions(request.getNumQuestions());
        config.setDurationMinutes(request.getDurationMinutes());
        config.setDifficultyFilter(blankToAll(request.getDifficultyFilter()));
        if (request.getIsActive() != null) {
            config.setIsActive(request.getIsActive());
        }
        TopicTestConfigResponse response = toResponse(configRepository.save(config));
        cacheEvictionService.evictCatalogData();
        cacheEvictionService.evictMockTestInfo(request.getTopicId());
        return response;
    }

    public void delete(Long id) {
        TopicTestConfig config = configRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Topic test config", id));
        purgeTopicMockTestData(config.getTopic().getId());
        configRepository.deleteById(id);
        cacheEvictionService.evictCatalogData();
        cacheEvictionService.evictMockTestInfo(config.getTopic().getId());
    }

    public void purgeTopicMockTestData(Long topicId) {
        testAttemptAnswerRepository.deleteByTopicId(topicId);
        testAttemptRepository.deleteByTopicId(topicId);
        questionBankService.deleteAllQuestionsForTopic(topicId);
    }

    @Transactional(readOnly = true)
    public TopicTestConfig resolveConfig(Long topicId) {
        return configRepository.findByTopicId(topicId)
            .filter(c -> Boolean.TRUE.equals(c.getIsActive()))
            .orElse(null);
    }

    private TopicTestConfigResponse defaultResponse(Long topicId) {
        Topic topic = topicRepository.findByIdWithChapterAndSubject(topicId)
            .orElseThrow(() -> new ResourceNotFoundException("Topic", topicId));
        return buildResponse(topic, null);
    }

    private TopicTestConfigResponse toResponse(TopicTestConfig config) {
        Topic topic = config.getTopic();
        return buildResponse(topic, config);
    }

    private TopicTestConfigResponse buildResponse(Topic topic, TopicTestConfig config) {
        Exam sheetExam = resolveSheetExam(topic.getChapter().getSubject().getId());
        boolean sheetBacked = sheetExam != null && googleSheetsService.hasSheetConfigured(sheetExam);
        long available = countAvailableQuestions(topic, config, sheetExam);
        int numQuestions = config != null ? config.getNumQuestions() : 10;
        int durationMinutes = config != null ? config.getDurationMinutes() : 15;
        String difficultyFilter = config != null && config.getDifficultyFilter() != null
            ? config.getDifficultyFilter() : "ALL";
        boolean isActive = config != null
            ? Boolean.TRUE.equals(config.getIsActive())
            : sheetBacked;
        int effectiveNum = googleSheetsService.resolveEffectiveQuestionCount(
            numQuestions, (int) available, sheetBacked);
        boolean canStart = googleSheetsService.canStartTest(
            numQuestions, (int) available, sheetBacked, isActive);

        return TopicTestConfigResponse.builder()
            .id(config != null ? config.getId() : null)
            .topicId(topic.getId())
            .topicTitle(topic.getTitle())
            .chapterId(topic.getChapter().getId())
            .chapterTitle(topic.getChapter().getTitle())
            .subjectId(topic.getChapter().getSubject().getId())
            .subjectName(topic.getChapter().getSubject().getName())
            .numQuestions(effectiveNum)
            .durationMinutes(durationMinutes)
            .difficultyFilter(difficultyFilter)
            .isActive(isActive)
            .availableQuestionCount(available)
            .canStart(canStart)
            .sheetBacked(sheetBacked)
            .createdAt(config != null ? config.getCreatedAt() : null)
            .build();
    }

    private long countAvailableQuestions(Topic topic, TopicTestConfig config, Exam sheetExam) {
        long dbCount = questionRepository.countByTopicIdAndIsActiveTrue(topic.getId());
        if (sheetExam == null) {
            return dbCount;
        }
        try {
            String sheetId = googleSheetsService.requireSheetId(sheetExam);
            List<SheetQuestion> pool = googleSheetsService.loadQuestions(sheetId);
            pool = googleSheetsService.filterForTopic(
                pool, sheetExam, topic.getChapter().getSubject(), topic);
            if (config != null) {
                pool = googleSheetsService.applyDifficultyFilter(pool, config.getDifficultyFilter());
            } else {
                pool = googleSheetsService.applyDifficultyFilter(pool, "ALL");
            }
            return Math.max(dbCount, pool.size());
        } catch (Exception ex) {
            log.warn("Failed to count sheet questions for topic {}: {}", topic.getId(), ex.getMessage());
            return dbCount;
        }
    }

    private Exam resolveSheetExam(Long subjectId) {
        for (ExamSubject link : examSubjectRepository.findBySubjectIdWithExamOrderByDisplayOrderAsc(subjectId)) {
            if (!Boolean.TRUE.equals(link.getIsActive())) {
                continue;
            }
            Exam exam = link.getExam();
            if (googleSheetsService.hasSheetConfigured(exam)) {
                return exam;
            }
        }
        return null;
    }

    private static String blankToAll(String value) {
        if (value == null || value.isBlank()) return "ALL";
        return value.trim().toUpperCase();
    }
}

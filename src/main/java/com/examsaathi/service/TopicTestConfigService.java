package com.examsaathi.service;

import com.examsaathi.dto.request.TopicTestConfigRequest;
import com.examsaathi.dto.response.TopicTestConfigResponse;
import com.examsaathi.entity.Topic;
import com.examsaathi.entity.TopicTestConfig;
import com.examsaathi.exception.BadRequestException;
import com.examsaathi.exception.ResourceNotFoundException;
import com.examsaathi.repository.QuestionRepository;
import com.examsaathi.repository.TestAttemptRepository;
import com.examsaathi.repository.TopicRepository;
import com.examsaathi.repository.TopicTestConfigRepository;
import com.examsaathi.service.QuestionBankService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class TopicTestConfigService {

    private final TopicTestConfigRepository configRepository;
    private final TopicRepository topicRepository;
    private final QuestionRepository questionRepository;
    private final TestAttemptRepository testAttemptRepository;
    private final QuestionBankService questionBankService;

    @Transactional(readOnly = true)
    public List<TopicTestConfigResponse> listAll() {
        return configRepository.findAll().stream().map(this::toResponse).toList();
    }

    @Transactional(readOnly = true)
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
        return toResponse(configRepository.save(config));
    }

    public void delete(Long id) {
        TopicTestConfig config = configRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Topic test config", id));
        if (testAttemptRepository.countByTopicTestConfigId(id) > 0) {
            throw new BadRequestException(
                "Topic test config cannot be deleted because it has test attempts. Deactivate it instead.");
        }
        questionBankService.clearActiveQuestionsForTopic(config.getTopic().getId());
        configRepository.deleteById(id);
    }

    @Transactional(readOnly = true)
    public TopicTestConfig resolveConfig(Long topicId) {
        return configRepository.findByTopicId(topicId)
            .filter(c -> Boolean.TRUE.equals(c.getIsActive()))
            .orElse(null);
    }

    private TopicTestConfigResponse defaultResponse(Long topicId) {
        Topic topic = topicRepository.findById(topicId)
            .orElseThrow(() -> new ResourceNotFoundException("Topic", topicId));
        return TopicTestConfigResponse.builder()
            .topicId(topicId)
            .topicTitle(topic.getTitle())
            .chapterId(topic.getChapter().getId())
            .chapterTitle(topic.getChapter().getTitle())
            .subjectId(topic.getChapter().getSubject().getId())
            .subjectName(topic.getChapter().getSubject().getName())
            .numQuestions(10)
            .durationMinutes(15)
            .difficultyFilter("ALL")
            .isActive(true)
            .availableQuestionCount(questionRepository.countByTopicIdAndIsActiveTrue(topicId))
            .build();
    }

    private TopicTestConfigResponse toResponse(TopicTestConfig config) {
        Topic topic = config.getTopic();
        return TopicTestConfigResponse.builder()
            .id(config.getId())
            .topicId(topic.getId())
            .topicTitle(topic.getTitle())
            .chapterId(topic.getChapter().getId())
            .chapterTitle(topic.getChapter().getTitle())
            .subjectId(topic.getChapter().getSubject().getId())
            .subjectName(topic.getChapter().getSubject().getName())
            .numQuestions(config.getNumQuestions())
            .durationMinutes(config.getDurationMinutes())
            .difficultyFilter(config.getDifficultyFilter() != null ? config.getDifficultyFilter() : "ALL")
            .isActive(config.getIsActive())
            .availableQuestionCount(questionRepository.countByTopicIdAndIsActiveTrue(topic.getId()))
            .createdAt(config.getCreatedAt())
            .build();
    }

    private static String blankToAll(String value) {
        if (value == null || value.isBlank()) return "ALL";
        return value.trim().toUpperCase();
    }
}

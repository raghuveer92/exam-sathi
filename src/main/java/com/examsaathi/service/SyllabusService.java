package com.examsaathi.service;

import com.examsaathi.dto.request.ChapterRequest;
import com.examsaathi.dto.request.TopicRequest;
import com.examsaathi.dto.response.ChapterResponse;
import com.examsaathi.dto.response.TopicResponse;
import com.examsaathi.entity.Chapter;
import com.examsaathi.entity.Subject;
import com.examsaathi.entity.Topic;
import com.examsaathi.exception.ResourceNotFoundException;
import com.examsaathi.repository.ChapterRepository;
import com.examsaathi.repository.SubjectRepository;
import com.examsaathi.repository.TopicRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class SyllabusService {

    private final ChapterRepository chapterRepository;
    private final TopicRepository topicRepository;
    private final SubjectRepository subjectRepository;
    private final UserMapper mapper;

    // ========== Chapters ==========

    @Transactional(readOnly = true)
    public List<ChapterResponse> getChaptersBySubject(Long subjectId) {
        return chapterRepository.findBySubjectIdAndIsActiveTrueOrderByOrderIndexAsc(subjectId)
            .stream().map(c -> mapper.toChapterResponse(c, false)).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public ChapterResponse getChapterById(Long id) {
        Chapter chapter = chapterRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Chapter", id));
        return mapper.toChapterResponse(chapter, true);
    }

    @Transactional
    public ChapterResponse createChapter(ChapterRequest request) {
        Subject subject = subjectRepository.findById(request.getSubjectId())
            .orElseThrow(() -> new ResourceNotFoundException("Subject", request.getSubjectId()));

        Chapter chapter = Chapter.builder()
            .subject(subject)
            .title(request.getTitle())
            .description(request.getDescription())
            .orderIndex(request.getOrderIndex())
            .isActive(request.getIsActive())
            .build();

        return mapper.toChapterResponse(chapterRepository.save(chapter), false);
    }

    @Transactional
    public ChapterResponse updateChapter(Long id, ChapterRequest request) {
        Chapter chapter = chapterRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Chapter", id));
        chapter.setTitle(request.getTitle());
        chapter.setDescription(request.getDescription());
        chapter.setOrderIndex(request.getOrderIndex());
        if (request.getIsActive() != null) chapter.setIsActive(request.getIsActive());
        return mapper.toChapterResponse(chapterRepository.save(chapter), false);
    }

    @Transactional
    public void deleteChapter(Long id) {
        if (!chapterRepository.existsById(id)) throw new ResourceNotFoundException("Chapter", id);
        chapterRepository.deleteById(id);
    }

    // ========== Topics ==========

    @Transactional(readOnly = true)
    public List<TopicResponse> getTopicsByChapter(Long chapterId) {
        return topicRepository.findByChapterIdAndIsActiveTrueOrderByOrderIndexAsc(chapterId)
            .stream().map(mapper::toTopicResponse).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public TopicResponse getTopicById(Long id) {
        Topic topic = topicRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Topic", id));
        return mapper.toTopicResponse(topic);
    }

    @Transactional
    public TopicResponse createTopic(TopicRequest request) {
        Chapter chapter = chapterRepository.findById(request.getChapterId())
            .orElseThrow(() -> new ResourceNotFoundException("Chapter", request.getChapterId()));

        Topic topic = Topic.builder()
            .chapter(chapter)
            .title(request.getTitle())
            .description(request.getDescription())
            .estimatedHours(request.getEstimatedHours())
            .difficultyLevel(request.getDifficultyLevel())
            .orderIndex(request.getOrderIndex())
            .isActive(request.getIsActive())
            .build();

        return mapper.toTopicResponse(topicRepository.save(topic));
    }

    @Transactional
    public TopicResponse updateTopic(Long id, TopicRequest request) {
        Topic topic = topicRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Topic", id));
        topic.setTitle(request.getTitle());
        topic.setDescription(request.getDescription());
        topic.setEstimatedHours(request.getEstimatedHours());
        topic.setDifficultyLevel(request.getDifficultyLevel());
        topic.setOrderIndex(request.getOrderIndex());
        if (request.getIsActive() != null) topic.setIsActive(request.getIsActive());
        return mapper.toTopicResponse(topicRepository.save(topic));
    }

    @Transactional
    public void deleteTopic(Long id) {
        if (!topicRepository.existsById(id)) throw new ResourceNotFoundException("Topic", id);
        topicRepository.deleteById(id);
    }
}

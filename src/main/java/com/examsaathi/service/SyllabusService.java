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
import com.examsaathi.repository.StudyProgressRepository;
import com.examsaathi.repository.SubjectRepository;
import com.examsaathi.repository.TopicRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class SyllabusService {

    private final ChapterRepository chapterRepository;
    private final TopicRepository topicRepository;
    private final SubjectRepository subjectRepository;
    private final StudyProgressRepository progressRepository;
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
        Topic topic = toTopicEntity(request, new HashMap<>());
        return mapper.toTopicResponse(topicRepository.save(topic));
    }

    @Transactional
    public List<TopicResponse> createTopics(List<TopicRequest> requests) {
        Map<Long, Chapter> chaptersById = new HashMap<>();
        List<Topic> topics = requests.stream()
            .map(request -> toTopicEntity(request, chaptersById))
            .toList();
        return topicRepository.saveAll(topics)
            .stream()
            .map(mapper::toTopicResponse)
            .collect(Collectors.toList());
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
        // Remove FK-dependent progress records first to avoid constraint violation
        progressRepository.deleteByTopicId(id);
        topicRepository.deleteById(id);
    }

    private Topic toTopicEntity(TopicRequest request, Map<Long, Chapter> chaptersById) {
        Chapter chapter = chaptersById.computeIfAbsent(
            request.getChapterId(),
            chapterId -> chapterRepository.findById(chapterId)
                .orElseThrow(() -> new ResourceNotFoundException("Chapter", chapterId))
        );

        return Topic.builder()
            .chapter(chapter)
            .title(request.getTitle())
            .description(request.getDescription())
            .estimatedHours(request.getEstimatedHours())
            .difficultyLevel(request.getDifficultyLevel())
            .orderIndex(request.getOrderIndex())
            .isActive(request.getIsActive())
            .build();
    }
}

package com.examsaathi.service;

import com.examsaathi.dto.response.*;
import com.examsaathi.entity.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.stream.Collectors;

/**
 * Mapper utility to convert entities to response DTOs.
 */
@Component
@RequiredArgsConstructor
public class UserMapper {

    public UserResponse toResponse(User user) {
        Integer daysUntilExam = null;
        if (user.getExamDate() != null) {
            long days = java.time.temporal.ChronoUnit.DAYS.between(java.time.LocalDate.now(), user.getExamDate());
            daysUntilExam = (int) days;
        }
        return UserResponse.builder()
            .id(user.getId())
            .email(user.getEmail())
            .fullName(user.getFullName())
            .phone(user.getPhone())
            .avatarUrl(user.getAvatarUrl())
            .selectedExamId(user.getSelectedExam() != null ? user.getSelectedExam().getId() : null)
            .selectedExamName(user.getSelectedExam() != null ? user.getSelectedExam().getName() : null)
            .targetCompletionDate(user.getTargetCompletionDate())
            .examDate(user.getExamDate())
            .syllabusTargetDate(user.getSyllabusTargetDate())
            .dailyTargetHours(user.getDailyTargetHours())
            .weeklyTargetHours(user.getWeeklyTargetHours())
            .daysUntilExam(daysUntilExam)
            .isActive(user.getIsActive())
            .studyStreakDays(user.getStudyStreakDays())
            .lastStudyDate(user.getLastStudyDate())
            .roles(user.getRoles().stream().map(r -> r.getName().name()).collect(Collectors.toList()))
            .createdAt(user.getCreatedAt())
            .build();
    }

    public ExamResponse toExamResponse(Exam exam, boolean includeSubjects) {
        return ExamResponse.builder()
            .id(exam.getId())
            .name(exam.getName())
            .description(exam.getDescription())
            .code(exam.getCode())
            .iconUrl(exam.getIconUrl())
            .colorCode(exam.getColorCode())
            .isActive(exam.getIsActive())
            .subjectCount(exam.getSubjects().size())
            .createdAt(exam.getCreatedAt())
            .subjects(includeSubjects
                ? exam.getSubjects().stream().map(s -> toSubjectResponse(s, false)).collect(Collectors.toList())
                : Collections.emptyList())
            .build();
    }

    public SubjectResponse toSubjectResponse(Subject subject, boolean includeChapters) {
        return SubjectResponse.builder()
            .id(subject.getId())
            .examId(subject.getExam().getId())
            .examName(subject.getExam().getName())
            .name(subject.getName())
            .description(subject.getDescription())
            .iconName(subject.getIconName())
            .colorCode(subject.getColorCode())
            .displayOrder(subject.getDisplayOrder())
            .isActive(subject.getIsActive())
            .topicCount(subject.getChapters().stream()
                .mapToInt(c -> c.getTopics().size()).sum())
            .chapters(includeChapters
                ? subject.getChapters().stream().map(c -> toChapterResponse(c, false)).collect(Collectors.toList())
                : Collections.emptyList())
            .build();
    }

    public ChapterResponse toChapterResponse(Chapter chapter, boolean includeTopics) {
        return ChapterResponse.builder()
            .id(chapter.getId())
            .subjectId(chapter.getSubject().getId())
            .subjectName(chapter.getSubject().getName())
            .title(chapter.getTitle())
            .description(chapter.getDescription())
            .orderIndex(chapter.getOrderIndex())
            .isActive(chapter.getIsActive())
            .topicCount(chapter.getTopics().size())
            .topics(includeTopics
                ? chapter.getTopics().stream().map(this::toTopicResponse).collect(Collectors.toList())
                : Collections.emptyList())
            .build();
    }

    public TopicResponse toTopicResponse(Topic topic) {
        return TopicResponse.builder()
            .id(topic.getId())
            .chapterId(topic.getChapter().getId())
            .chapterTitle(topic.getChapter().getTitle())
            .title(topic.getTitle())
            .description(topic.getDescription())
            .estimatedHours(topic.getEstimatedHours())
            .difficultyLevel(topic.getDifficultyLevel())
            .orderIndex(topic.getOrderIndex())
            .isActive(topic.getIsActive())
            .build();
    }

    public DailyStudyLogResponse toDailyLogResponse(DailyStudyLog log) {
        return DailyStudyLogResponse.builder()
            .id(log.getId())
            .studyDate(log.getStudyDate())
            .hoursStudied(log.getHoursStudied())
            .topicsCompleted(log.getTopicsCompleted())
            .build();
    }
}

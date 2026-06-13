package com.examsaathi.service;

import com.examsaathi.dto.response.*;
import com.examsaathi.entity.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
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

        List<UserExamResponse> userExams = user.getUserExams() == null
            ? Collections.emptyList()
            : user.getUserExams().stream()
                .sorted(Comparator.comparing(UserExam::getCreatedAt, Comparator.nullsLast(Comparator.naturalOrder())))
                .map(this::toUserExamResponse)
                .collect(Collectors.toList());

        Long activeUserExamId = user.getUserExams() == null
            ? null
            : user.getUserExams().stream()
                .filter(ue -> Boolean.TRUE.equals(ue.getIsActive()))
                .map(UserExam::getId)
                .findFirst()
                .orElse(null);

        Double dailyTargetHours = user.getDailyTargetHours();
        Double weeklyTargetHours = user.getWeeklyTargetHours();
        if ((dailyTargetHours == null || dailyTargetHours <= 0) && user.getUserExams() != null) {
            UserExam activeExam = user.getUserExams().stream()
                .filter(ue -> activeUserExamId != null
                    ? ue.getId().equals(activeUserExamId)
                    : Boolean.TRUE.equals(ue.getIsActive()))
                .findFirst()
                .orElse(null);
            if (activeExam != null && activeExam.getDailyTargetHours() != null) {
                dailyTargetHours = activeExam.getDailyTargetHours();
                weeklyTargetHours = activeExam.getWeeklyTargetHours();
            }
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
            .dailyTargetHours(dailyTargetHours)
            .weeklyTargetHours(weeklyTargetHours)
            .daysUntilExam(daysUntilExam)
            .isActive(user.getIsActive())
            .isEmailVerified(user.getIsEmailVerified())
            .studyStreakDays(user.getStudyStreakDays())
            .lastStudyDate(user.getLastStudyDate())
            .roles(user.getRoles().stream().map(r -> r.getName().name()).collect(Collectors.toList()))
            .activeUserExamId(activeUserExamId)
            .userExams(userExams)
            .authProvider(user.getAuthProvider() != null ? user.getAuthProvider().name() : "EMAIL")
            .createdAt(user.getCreatedAt())
            .build();
    }

    public UserExamResponse toUserExamResponse(UserExam userExam) {
        Integer daysLeft = null;
        if (userExam.getExamDate() != null) {
            daysLeft = (int) java.time.temporal.ChronoUnit.DAYS.between(LocalDate.now(), userExam.getExamDate());
        }
        return UserExamResponse.builder()
            .id(userExam.getId())
            .examId(userExam.getExam().getId())
            .examName(userExam.getExam().getName())
            .examDate(userExam.getExamDate())
            .dailyTargetHours(userExam.getDailyTargetHours())
            .experienceLevel(userExam.getExperienceLevel())
            .daysLeft(daysLeft)
            .isActive(userExam.getIsActive())
            .createdAt(userExam.getCreatedAt())
            .build();
    }

    public ExamResponse toExamResponse(Exam exam, boolean includeSubjects) {
        List<ExamSubject> activeExamSubjects = exam.getExamSubjects() == null
            ? new ArrayList<>()
            : exam.getExamSubjects().stream()
                .filter(es -> Boolean.TRUE.equals(es.getIsActive()) && Boolean.TRUE.equals(es.getSubject().getIsActive()))
                .sorted(Comparator.comparing(ExamSubject::getDisplayOrder))
                .collect(Collectors.toList());

        return ExamResponse.builder()
            .id(exam.getId())
            .name(exam.getName())
            .description(exam.getDescription())
            .shortDescription(exam.getShortDescription())
            .categoryId(exam.getCategory() != null ? exam.getCategory().getId() : null)
            .categoryName(exam.getCategory() != null ? exam.getCategory().getName() : null)
            .bannerUrl(exam.getBannerUrl())
            .difficultyLevel(exam.getDifficultyLevel())
            .featured(exam.getFeatured())
            .popular(exam.getPopular())
            .displayOrder(exam.getDisplayOrder())
            .featuredOrder(exam.getFeaturedOrder())
            .code(exam.getCode())
            .iconUrl(exam.getIconUrl())
            .colorCode(exam.getColorCode())
            .isActive(exam.getIsActive())
            .subjectCount(activeExamSubjects.size())
            .createdAt(exam.getCreatedAt())
            .updatedAt(exam.getUpdatedAt())
            .subjects(includeSubjects
                ? activeExamSubjects.stream().map(es -> toSubjectResponse(es, false)).collect(Collectors.toList())
                : Collections.emptyList())
            .build();
    }

    public SubjectResponse toSubjectResponse(Subject subject, boolean includeChapters) {
        return toSubjectResponse(subject, null, null, includeChapters, false);
    }

    public SubjectResponse toSubjectResponse(ExamSubject examSubject, boolean includeChapters) {
        return toSubjectResponse(examSubject.getSubject(), examSubject, null, includeChapters, false);
    }

    public SubjectResponse toSubjectResponse(ExamSubject examSubject, ExamSubjectGroup group, boolean includeChapters, boolean selected) {
        return toSubjectResponse(examSubject.getSubject(), examSubject, group, includeChapters, selected);
    }

    private SubjectResponse toSubjectResponse(Subject subject, ExamSubject examSubject, ExamSubjectGroup group, boolean includeChapters, boolean selected) {
        return SubjectResponse.builder()
            .id(subject.getId())
            .examId(examSubject != null ? examSubject.getExam().getId() : null)
            .examName(examSubject != null ? examSubject.getExam().getName() : null)
            .name(subject.getName())
            .description(subject.getDescription())
            .iconName(subject.getIconName())
            .colorCode(subject.getColorCode())
            .displayOrder(examSubject != null ? examSubject.getDisplayOrder() : 0)
            .isActive(subject.getIsActive())
            .groupId(group != null ? group.getId() : null)
            .groupName(group != null ? group.getGroupName() : null)
            .groupOptional(group != null ? group.getIsOptional() : null)
            .minSelection(group != null ? group.getMinSelection() : null)
            .maxSelection(group != null ? group.getMaxSelection() : null)
            .selected(selected)
            .createdAt(subject.getCreatedAt())
            .updatedAt(subject.getUpdatedAt())
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
            .updatedAt(chapter.getUpdatedAt())
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
            .updatedAt(topic.getUpdatedAt())
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

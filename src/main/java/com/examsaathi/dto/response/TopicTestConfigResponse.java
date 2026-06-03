package com.examsaathi.dto.response;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class TopicTestConfigResponse {
    private Long id;
    private Long topicId;
    private String topicTitle;
    private Long chapterId;
    private String chapterTitle;
    private Long subjectId;
    private String subjectName;
    private Integer numQuestions;
    private Integer durationMinutes;
    private String difficultyFilter;
    private Boolean isActive;
    private Long availableQuestionCount;
    private LocalDateTime createdAt;
}

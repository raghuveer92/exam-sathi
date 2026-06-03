package com.examsaathi.dto.response;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
public class QuestionResponse {
    private Long id;
    private Long examId;
    private String examName;
    private Long subjectId;
    private String subjectName;
    private Long chapterId;
    private String chapterTitle;
    private Long topicId;
    private String topicTitle;
    private String questionText;
    private String questionType;
    private String explanation;
    private Double marks;
    private Double negativeMarks;
    private String difficultyLevel;
    private Boolean previousYear;
    private String previousYearValue;
    private Boolean isActive;
    private List<QuestionOptionResponse> options;
    private LocalDateTime createdAt;
}

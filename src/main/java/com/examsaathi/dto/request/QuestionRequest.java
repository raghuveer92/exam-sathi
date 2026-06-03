package com.examsaathi.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.List;

@Data
public class QuestionRequest {

    @NotNull
    private Long examId;

    @NotNull
    private Long subjectId;

    @NotNull
    private Long chapterId;

    @NotNull
    private Long topicId;

    @NotBlank
    private String questionText;

    @NotBlank
    private String questionType;

    private String explanation;

    @NotNull
    private Double marks;

    private Double negativeMarks;

    private String difficultyLevel;

    private Boolean previousYear;

    private String previousYearValue;

    private Boolean isActive;

    @NotNull
    @Size(min = 2)
    private List<QuestionOptionRequest> options;
}

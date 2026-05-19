package com.examsaathi.dto.response;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
public class ExamResponse {
    private Long id;
    private String name;
    private String description;
    private String code;
    private String iconUrl;
    private String colorCode;
    private Boolean isActive;
    private int subjectCount;
    private LocalDateTime createdAt;
    private List<SubjectResponse> subjects;
}

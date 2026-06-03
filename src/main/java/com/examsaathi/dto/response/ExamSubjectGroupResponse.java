package com.examsaathi.dto.response;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
public class ExamSubjectGroupResponse {
    private Long id;
    private Long examId;
    private String groupName;
    private Boolean isOptional;
    private Integer minSelection;
    private Integer maxSelection;
    private Integer displayOrder;
    private Integer selectedCount;
    private LocalDateTime createdAt;
    private List<SubjectResponse> subjects;
}
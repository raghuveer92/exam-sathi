package com.examsaathi.dto.response;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
public class SubjectResponse {
    private Long id;
    private Long examId;
    private String examName;
    private String name;
    private String description;
    private String iconName;
    private String colorCode;
    private Integer displayOrder;
    private Boolean isActive;
    private Long groupId;
    private String groupName;
    private Boolean groupOptional;
    private Integer minSelection;
    private Integer maxSelection;
    private Boolean selected;
    private int topicCount;
    private Double totalEstimatedHours;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private List<ChapterResponse> chapters;
}

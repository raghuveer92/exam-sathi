package com.examsaathi.dto.response;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class SubjectProgressResponse {
    private Long subjectId;
    private String subjectName;
    private String iconName;
    private String colorCode;
    private Integer displayOrder;
    private int totalTopics;
    private int completedTopics;
    private double completionPercent;
    private double totalEstimatedHours;
    private double completedHours;
}

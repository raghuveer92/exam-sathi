package com.examsaathi.dto.response;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class SubjectDetailResponse {
    private Long subjectId;
    private String subjectName;
    private String iconName;
    private String colorCode;
    private int totalTopics;
    private int completedTopics;
    private double completionPercent;
    private double totalStudyHours;
    private List<ChapterWithProgressResponse> chapters;
}

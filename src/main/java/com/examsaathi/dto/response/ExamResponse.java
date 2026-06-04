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
    private String shortDescription;
    private Long categoryId;
    private String categoryName;
    private String bannerUrl;
    private String difficultyLevel;
    private Boolean featured;
    private Boolean popular;
    private Integer displayOrder;
    private Integer featuredOrder;
    private String code;
    private String iconUrl;
    private String colorCode;
    private Boolean isActive;
    private int subjectCount;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private List<SubjectResponse> subjects;
}

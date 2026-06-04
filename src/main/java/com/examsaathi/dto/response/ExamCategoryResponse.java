package com.examsaathi.dto.response;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class ExamCategoryResponse {
    private Long id;
    private String name;
    private String description;
    private String icon;
    private Integer displayOrder;
    private Boolean isActive;
    private int examCount;
    private LocalDateTime createdAt;
}

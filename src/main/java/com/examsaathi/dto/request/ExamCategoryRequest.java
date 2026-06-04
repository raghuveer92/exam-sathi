package com.examsaathi.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class ExamCategoryRequest {
    @NotBlank
    @Size(max = 100)
    private String name;

    @Size(max = 500)
    private String description;

    @Size(max = 80)
    private String icon;

    private Integer displayOrder = 0;
    private Boolean isActive = true;
}

package com.examsaathi.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class ExamRequest {

    @NotBlank(message = "Exam name is required")
    @Size(max = 100)
    private String name;

    @Size(max = 500)
    private String description;

    @Size(max = 30)
    private String code;

    @Size(max = 500)
    private String iconUrl;

    /** Hex color e.g. #6C63FF */
    @Size(max = 10)
    private String colorCode;

    private Boolean isActive = true;
}

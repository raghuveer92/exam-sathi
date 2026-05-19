package com.examsaathi.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class ChapterRequest {

    @NotNull(message = "Subject ID is required")
    private Long subjectId;

    @NotBlank(message = "Title is required")
    @Size(max = 200)
    private String title;

    @Size(max = 1000)
    private String description;

    private Integer orderIndex = 0;

    private Boolean isActive = true;
}

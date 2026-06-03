package com.examsaathi.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class SubjectRequest {

    @NotNull(message = "Exam ID is required")
    private Long examId;

    @NotBlank(message = "Subject name is required")
    @Size(max = 100)
    private String name;

    @Size(max = 500)
    private String description;

    /** Material icon name e.g. calculate, science, menu_book */
    @NotBlank(message = "Icon name is required")
    @Size(max = 100)
    private String iconName;

    /** Hex color e.g. #1565C0 */
    @NotBlank(message = "Color code is required")
    @Size(max = 10)
    private String colorCode;

    private Integer displayOrder = 0;

    private Boolean isActive = true;

    private Long groupId;
}

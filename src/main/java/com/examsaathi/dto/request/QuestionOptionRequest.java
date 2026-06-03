package com.examsaathi.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class QuestionOptionRequest {
    @NotBlank
    private String optionKey;
    @NotBlank
    private String optionText;
    private Boolean isCorrect;
    private Integer displayOrder;
}

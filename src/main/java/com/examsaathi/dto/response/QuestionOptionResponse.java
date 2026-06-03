package com.examsaathi.dto.response;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class QuestionOptionResponse {
    private Long id;
    private String optionKey;
    private String optionText;
    private Boolean isCorrect;
    private Integer displayOrder;
}

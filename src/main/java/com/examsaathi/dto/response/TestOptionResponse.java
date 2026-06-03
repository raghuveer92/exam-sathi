package com.examsaathi.dto.response;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class TestOptionResponse {
    private Long id;
    private String optionKey;
    private String optionText;
}

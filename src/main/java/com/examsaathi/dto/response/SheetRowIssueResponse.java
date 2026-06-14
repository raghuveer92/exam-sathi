package com.examsaathi.dto.response;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class SheetRowIssueResponse {
    private int rowNumber;
    private String questionId;
    private String issue;
}

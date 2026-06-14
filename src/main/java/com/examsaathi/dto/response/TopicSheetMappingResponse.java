package com.examsaathi.dto.response;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class TopicSheetMappingResponse {
    private Long topicId;
    private String topicTitle;
    private Long subjectId;
    private String subjectName;
    private int sheetQuestionCount;
    private boolean matchedById;
    private boolean matchedByName;
    private boolean sufficientForTest;
    private int requiredQuestionCount;
}

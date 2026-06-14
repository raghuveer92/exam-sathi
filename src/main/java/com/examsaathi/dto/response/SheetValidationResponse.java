package com.examsaathi.dto.response;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class SheetValidationResponse {
    private Long examId;
    private String examName;
    private String sheetId;
    private boolean valid;
    private int totalRows;
    private int validQuestionRows;
    private int invalidQuestionRows;
    private List<String> missingColumns;
    private List<String> structureErrors;
    private List<SheetRowIssueResponse> rowIssues;
    private List<TopicSheetMappingResponse> topicMappings;
    private List<String> sheetTopicsWithoutDbMatch;
    private List<String> dbTopicsWithoutSheetQuestions;
    private boolean usesIdBasedMatching;
    private String matchingStrategy;
}

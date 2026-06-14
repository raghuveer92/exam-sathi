package com.examsaathi.dto.response;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class SheetCacheRefreshResponse {
    private String sheetId;
    private Long examId;
    private int questionCount;
    private boolean refreshed;
}

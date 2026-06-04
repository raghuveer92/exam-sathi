package com.examsaathi.dto.response;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Data
@Builder
public class SyncBundleResponse {
    private LocalDateTime serverTime;
    private boolean fullSync;
    private DashboardResponse dashboard;
    private List<UserExamResponse> myExams;
    /** examId -> subject progress rows */
    private Map<Long, List<SubjectProgressResponse>> subjectProgressByExamId;
    private List<StudyProgressSyncItem> changedProgress;
}

package com.examsaathi.dto.response;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
public class SyncCatalogResponse {
    private LocalDateTime serverTime;
    private boolean fullSync;
    private List<ExamCategoryResponse> categories;
    private List<ExamResponse> exams;
    private List<SubjectResponse> subjects;
    private List<ChapterResponse> chapters;
    private List<TopicResponse> topics;
}

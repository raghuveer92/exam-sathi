package com.examsaathi.dto.response;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
public class ChapterResponse {
    private Long id;
    private Long subjectId;
    private String subjectName;
    private String title;
    private String description;
    private Integer orderIndex;
    private Boolean isActive;
    private int topicCount;
    private List<TopicResponse> topics;
    private LocalDateTime updatedAt;
}

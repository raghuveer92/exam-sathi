package com.examsaathi.dto.response;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class ChapterWithProgressResponse {
    private Long id;
    private String title;
    private String description;
    private Integer orderIndex;
    private int totalTopics;
    private int completedTopics;
    private double completionPercent;
    private List<TopicResponse> topics;
}

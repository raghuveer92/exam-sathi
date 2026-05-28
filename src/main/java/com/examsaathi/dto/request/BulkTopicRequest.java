package com.examsaathi.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

import java.util.List;

@Data
public class BulkTopicRequest {

    @Valid
    @NotEmpty(message = "At least one topic is required")
    private List<TopicRequest> topics;
}
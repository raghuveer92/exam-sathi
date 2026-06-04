package com.examsaathi.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.util.Map;

@Data
public class SyncPushItem {
    /** Client-generated idempotency key */
    private String clientId;

    @NotBlank
    private String type;

    private Map<String, Object> payload;
}

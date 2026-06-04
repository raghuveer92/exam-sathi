package com.examsaathi.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

import java.util.List;

@Data
public class SyncPushRequest {
    @NotEmpty
    @Valid
    private List<SyncPushItem> items;
}

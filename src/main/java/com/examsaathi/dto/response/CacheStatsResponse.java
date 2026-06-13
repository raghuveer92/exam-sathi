package com.examsaathi.dto.response;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class CacheStatsResponse {
    private String cacheName;
    private long hitCount;
    private long missCount;
    private long entryCount;
    private double hitRatio;
}

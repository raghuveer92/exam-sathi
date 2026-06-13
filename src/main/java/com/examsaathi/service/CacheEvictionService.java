package com.examsaathi.service;

import com.examsaathi.config.CacheNames;
import com.examsaathi.dto.response.CacheStatsResponse;
import com.github.benmanes.caffeine.cache.stats.CacheStats;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Caching;
import org.springframework.cache.caffeine.CaffeineCache;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.Comparator;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class CacheEvictionService {

    private final CacheManager cacheManager;

    @Caching(evict = {
        @CacheEvict(value = CacheNames.EXAMS, allEntries = true),
        @CacheEvict(value = CacheNames.EXAM_LIST, allEntries = true),
        @CacheEvict(value = CacheNames.SUBJECTS_BY_EXAM, allEntries = true),
        @CacheEvict(value = CacheNames.SUBJECTS, allEntries = true),
        @CacheEvict(value = CacheNames.CHAPTERS_BY_SUBJECT, allEntries = true),
        @CacheEvict(value = CacheNames.CHAPTERS, allEntries = true),
        @CacheEvict(value = CacheNames.TOPICS_BY_CHAPTER, allEntries = true),
        @CacheEvict(value = CacheNames.TOPICS_BY_EXAM, allEntries = true),
        @CacheEvict(value = CacheNames.TOPICS, allEntries = true),
        @CacheEvict(value = CacheNames.EXAM_CATALOG, allEntries = true),
        @CacheEvict(value = CacheNames.EXAM_CATEGORIES, allEntries = true),
        @CacheEvict(value = CacheNames.SYNC_CATALOG, allEntries = true),
        @CacheEvict(value = CacheNames.MOCK_TEST_INFO, allEntries = true),
        @CacheEvict(value = CacheNames.TOPIC_TEST_CONFIG, allEntries = true),
        @CacheEvict(value = CacheNames.EXAM_SUBJECT_GROUPS, allEntries = true),
        @CacheEvict(value = CacheNames.CURRENT_AFFAIRS, allEntries = true)
    })
    public void evictCatalogData() {
        log.debug("Evicted catalog-related caches");
    }

    @CacheEvict(value = CacheNames.DASHBOARD, key = "#userId")
    public void evictDashboard(Long userId) {
        log.debug("Evicted dashboard cache for userId={}", userId);
    }

    @CacheEvict(value = CacheNames.DASHBOARD, allEntries = true)
    public void evictAllDashboards() {
        log.debug("Evicted all dashboard caches");
    }

    @Caching(evict = {
        @CacheEvict(value = CacheNames.LEADERBOARD, allEntries = true),
        @CacheEvict(value = CacheNames.ANALYTICS, allEntries = true)
    })
    public void evictLeaderboardAndAnalytics() {
        log.debug("Evicted leaderboard and analytics caches");
    }

    @CacheEvict(value = CacheNames.MOCK_TEST_INFO, key = "'topic_' + #topicId")
    public void evictMockTestInfo(Long topicId) {
        log.debug("Evicted mock test info for topicId={}", topicId);
    }

    public void clearAllCaches() {
        for (String name : CacheNames.ALL_CACHES) {
            Cache cache = cacheManager.getCache(name);
            if (cache != null) {
                cache.clear();
            }
        }
        log.info("Cleared all application caches");
    }

    public List<CacheStatsResponse> getCacheStats() {
        return Arrays.stream(CacheNames.ALL_CACHES)
            .map(this::statsFor)
            .sorted(Comparator.comparing(CacheStatsResponse::getCacheName))
            .toList();
    }

    private CacheStatsResponse statsFor(String cacheName) {
        Cache cache = cacheManager.getCache(cacheName);
        if (!(cache instanceof CaffeineCache caffeineCache)) {
            return CacheStatsResponse.builder()
                .cacheName(cacheName)
                .hitCount(0)
                .missCount(0)
                .entryCount(0)
                .hitRatio(0.0)
                .build();
        }

        com.github.benmanes.caffeine.cache.Cache<Object, Object> nativeCache =
            caffeineCache.getNativeCache();
        CacheStats stats = nativeCache.stats();
        long hits = stats.hitCount();
        long misses = stats.missCount();
        long total = hits + misses;

        return CacheStatsResponse.builder()
            .cacheName(cacheName)
            .hitCount(hits)
            .missCount(misses)
            .entryCount(nativeCache.estimatedSize())
            .hitRatio(total == 0 ? 0.0 : Math.round((hits * 1000.0 / total)) / 1000.0)
            .build();
    }
}

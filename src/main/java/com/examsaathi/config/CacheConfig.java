package com.examsaathi.config;

import com.github.benmanes.caffeine.cache.Caffeine;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.caffeine.CaffeineCache;
import org.springframework.cache.support.SimpleCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

@Configuration
@EnableCaching
public class CacheConfig {

    private static final int DEFAULT_MAX_SIZE = 10_000;
    private static final int DEFAULT_TTL_MINUTES = 30;
    private static final int LONG_TTL_HOURS = 6;
    private static final int SHORT_TTL_MINUTES = 5;
    private static final int CURRENT_AFFAIRS_TTL_HOURS = 24;

    @Bean
    public CacheManager cacheManager() {
        List<CaffeineCache> caches = new ArrayList<>();
        for (String name : CacheNames.ALL_CACHES) {
            caches.add(new CaffeineCache(name, caffeineFor(name).build()));
        }
        SimpleCacheManager manager = new SimpleCacheManager();
        manager.setCaches(caches);
        return manager;
    }

    private Caffeine<Object, Object> caffeineFor(String cacheName) {
        Caffeine<Object, Object> builder = Caffeine.newBuilder()
            .maximumSize(DEFAULT_MAX_SIZE)
            .recordStats();

        return switch (cacheName) {
            case CacheNames.EXAMS, CacheNames.EXAM_LIST, CacheNames.SUBJECTS_BY_EXAM,
                 CacheNames.SUBJECTS, CacheNames.CHAPTERS_BY_SUBJECT, CacheNames.CHAPTERS,
                 CacheNames.TOPICS_BY_CHAPTER, CacheNames.TOPICS_BY_EXAM, CacheNames.TOPICS,
                 CacheNames.MOCK_TEST_INFO, CacheNames.TOPIC_TEST_CONFIG,
                 CacheNames.EXAM_SUBJECT_GROUPS ->
                builder.expireAfterWrite(LONG_TTL_HOURS, TimeUnit.HOURS);
            case CacheNames.DASHBOARD, CacheNames.LEADERBOARD, CacheNames.ANALYTICS ->
                builder.expireAfterWrite(SHORT_TTL_MINUTES, TimeUnit.MINUTES);
            case CacheNames.CURRENT_AFFAIRS ->
                builder.expireAfterWrite(CURRENT_AFFAIRS_TTL_HOURS, TimeUnit.HOURS);
            default ->
                builder.expireAfterWrite(DEFAULT_TTL_MINUTES, TimeUnit.MINUTES);
        };
    }
}

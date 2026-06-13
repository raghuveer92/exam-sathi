package com.examsaathi.config;

import com.examsaathi.service.CacheEvictionService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
class CacheManagementTest {

    @Autowired
    private CacheManager cacheManager;

    @Autowired
    private CacheEvictionService cacheEvictionService;

    @Test
    void cacheMissThenHit_storesValueInExamsCache() {
        Cache cache = cacheManager.getCache(CacheNames.EXAMS);
        assertThat(cache).isNotNull();

        cache.put(CacheKeyBuilder.exam(99L), "cached-exam");
        assertThat(cache.get(CacheKeyBuilder.exam(99L))).isNotNull();
        assertThat(cache.get(CacheKeyBuilder.exam(99L)).get()).isEqualTo("cached-exam");
    }

    @Test
    void evictCatalogData_clearsSyncCatalogCache() {
        Cache cache = cacheManager.getCache(CacheNames.SYNC_CATALOG);
        assertThat(cache).isNotNull();
        cache.put("full", "payload");

        cacheEvictionService.evictCatalogData();

        assertThat(cache.get("full")).isNull();
    }

    @Test
    void clearAllCaches_removesDashboardEntries() {
        Cache dashboard = cacheManager.getCache(CacheNames.DASHBOARD);
        assertThat(dashboard).isNotNull();
        dashboard.put(42L, "summary");

        cacheEvictionService.clearAllCaches();

        assertThat(dashboard.get(42L)).isNull();
    }

    @Test
    void evictDashboard_removesOnlyTargetUser() {
        Cache dashboard = cacheManager.getCache(CacheNames.DASHBOARD);
        dashboard.put(1L, "user-1");
        dashboard.put(2L, "user-2");

        cacheEvictionService.evictDashboard(1L);

        assertThat(dashboard.get(1L)).isNull();
        assertThat(dashboard.get(2L)).isNotNull();
    }

    @Test
    void getCacheStats_returnsRegisteredCaches() {
        var stats = cacheEvictionService.getCacheStats();
        assertThat(stats).isNotEmpty();
        assertThat(stats.stream().map(s -> s.getCacheName()).toList())
            .contains(CacheNames.EXAMS, CacheNames.DASHBOARD, CacheNames.SYNC_CATALOG);
    }

    @Test
    void evictLeaderboardAndAnalytics_clearsAnalyticsCache() {
        Cache analytics = cacheManager.getCache(CacheNames.ANALYTICS);
        analytics.put("admin", "stats");

        cacheEvictionService.evictLeaderboardAndAnalytics();

        assertThat(analytics.get("admin")).isNull();
    }
}

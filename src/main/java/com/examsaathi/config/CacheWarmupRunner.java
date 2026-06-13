package com.examsaathi.config;

import com.examsaathi.service.ExamCatalogService;
import com.examsaathi.service.ExamCategoryService;
import com.examsaathi.service.ExamService;
import com.examsaathi.service.SyncService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(name = "app.cache.warmup-enabled", havingValue = "true", matchIfMissing = true)
@RequiredArgsConstructor
@Slf4j
public class CacheWarmupRunner implements ApplicationRunner {

    private final ExamService examService;
    private final ExamCategoryService categoryService;
    private final ExamCatalogService catalogService;
    private final SyncService syncService;

    @Override
    public void run(ApplicationArguments args) {
        try {
            examService.getAllActiveExams();
            categoryService.getActiveCategories();
            catalogService.getFeatured();
            catalogService.getCatalog(null);
            syncService.getCatalogSync(null, null);
            log.info("Cache warmup completed for exams, categories, catalog, and sync catalog");
        } catch (Exception e) {
            log.warn("Cache warmup skipped due to error: {}", e.getMessage());
        }
    }
}

package com.examsaathi.config;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

public final class CacheKeyBuilder {

    private CacheKeyBuilder() {}

    public static String exam(Long id) {
        return "exam_" + id;
    }

    public static String syncCatalog(LocalDateTime since, List<Long> examIds) {
        if (since != null) {
            return "delta:" + since;
        }
        List<Long> scoped = normalizeExamIds(examIds);
        return scoped.isEmpty() ? "full" : "exams:" + scoped.stream()
            .map(String::valueOf)
            .collect(Collectors.joining(","));
    }

    private static List<Long> normalizeExamIds(List<Long> examIds) {
        if (examIds == null || examIds.isEmpty()) {
            return List.of();
        }
        return examIds.stream()
            .filter(Objects::nonNull)
            .distinct()
            .sorted()
            .collect(Collectors.toList());
    }
}

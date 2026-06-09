package com.examsaathi.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.util.FileCopyUtils;

import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.regex.Pattern;

/**
 * Applies performance indexes used by the student Flutter app APIs.
 * Runs on startup for shared DBs where Flyway is not enabled.
 */
@Component
@Order(2)
@RequiredArgsConstructor
@Slf4j
public class FrontendApiIndexMigration implements CommandLineRunner {

    private static final Pattern STATEMENT_SPLIT =
        Pattern.compile(";\\s*(?=\\n|$)");

    private final JdbcTemplate jdbcTemplate;

    @Override
    public void run(String... args) {
        try {
            var resource = new ClassPathResource("db/migration/V9__frontend_api_indexes.sql");
            var sql = FileCopyUtils.copyToString(
                new InputStreamReader(resource.getInputStream(), StandardCharsets.UTF_8));
            var applied = 0;
            for (var statement : STATEMENT_SPLIT.split(sql)) {
                var trimmed = stripLeadingComments(statement).trim();
                if (trimmed.isEmpty()) {
                    continue;
                }
                jdbcTemplate.execute(trimmed);
                applied++;
            }
            log.info("Frontend API index migration applied ({} statements)", applied);
        } catch (Exception e) {
            log.warn("Frontend API index migration skipped or partial: {}", e.getMessage());
        }
    }

    private static String stripLeadingComments(String statement) {
        var lines = statement.lines().toList();
        var start = 0;
        while (start < lines.size() && lines.get(start).trim().startsWith("--")) {
            start++;
        }
        return String.join("\n", lines.subList(start, lines.size()));
    }
}

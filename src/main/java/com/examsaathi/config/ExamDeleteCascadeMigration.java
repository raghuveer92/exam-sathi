package com.examsaathi.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/**
 * Applies exam-delete FK cascades on shared DBs where Flyway is not enabled.
 */
@Component
@Order(1)
@RequiredArgsConstructor
@Slf4j
public class ExamDeleteCascadeMigration implements CommandLineRunner {

    private final JdbcTemplate jdbcTemplate;

    @Override
    public void run(String... args) {
        try {
            dropForeignKeyOnColumn("user_exam", "exam_id");
            jdbcTemplate.execute("""
                ALTER TABLE user_exam
                ADD CONSTRAINT fk_user_exam_exam
                FOREIGN KEY (exam_id) REFERENCES exams(id) ON DELETE CASCADE
                """);

            dropForeignKeyOnColumn("questions", "exam_id");
            jdbcTemplate.execute("""
                ALTER TABLE questions
                ADD CONSTRAINT questions_exam_id_fkey
                FOREIGN KEY (exam_id) REFERENCES exams(id) ON DELETE CASCADE
                """);

            dropForeignKeyOnColumn("test_attempt_answers", "question_id");
            jdbcTemplate.execute("""
                ALTER TABLE test_attempt_answers
                ADD CONSTRAINT test_attempt_answers_question_id_fkey
                FOREIGN KEY (question_id) REFERENCES questions(id) ON DELETE CASCADE
                """);

            dropForeignKeyOnColumn("users", "selected_exam_id");
            jdbcTemplate.execute("""
                ALTER TABLE users
                ADD CONSTRAINT fk_users_selected_exam
                FOREIGN KEY (selected_exam_id) REFERENCES exams(id) ON DELETE SET NULL
                """);

            dropForeignKeyOnColumn("exam_subjects", "exam_id");
            jdbcTemplate.execute("""
                ALTER TABLE exam_subjects
                ADD CONSTRAINT fk_exam_subjects_exam
                FOREIGN KEY (exam_id) REFERENCES exams(id) ON DELETE CASCADE
                """);

            dropForeignKeyOnColumn("exam_subject_groups", "exam_id");
            jdbcTemplate.execute("""
                ALTER TABLE exam_subject_groups
                ADD CONSTRAINT fk_exam_subject_groups_exam
                FOREIGN KEY (exam_id) REFERENCES exams(id) ON DELETE CASCADE
                """);

            log.info("Exam delete cascade migration applied");
        } catch (Exception e) {
            log.warn("Exam delete cascade migration skipped or partial: {}", e.getMessage());
        }
    }

    private void dropForeignKeyOnColumn(String table, String column) {
        jdbcTemplate.queryForList("""
            SELECT tc.constraint_name
            FROM information_schema.table_constraints tc
            JOIN information_schema.key_column_usage kcu
              ON tc.constraint_name = kcu.constraint_name
             AND tc.table_schema = kcu.table_schema
            WHERE tc.constraint_type = 'FOREIGN KEY'
              AND tc.table_name = ?
              AND kcu.column_name = ?
            """, table, column)
            .forEach(row -> {
                String constraint = (String) row.get("constraint_name");
                jdbcTemplate.execute(
                    "ALTER TABLE " + table + " DROP CONSTRAINT IF EXISTS " + constraint);
            });
    }
}

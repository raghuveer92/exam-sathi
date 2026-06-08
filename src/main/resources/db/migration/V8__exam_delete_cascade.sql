-- Ensure exam deletion cascades cleanly from admin panel.

-- Mock-test answers must not block question / exam removal.
ALTER TABLE test_attempt_answers
    DROP CONSTRAINT IF EXISTS test_attempt_answers_question_id_fkey;

ALTER TABLE test_attempt_answers
    ADD CONSTRAINT test_attempt_answers_question_id_fkey
    FOREIGN KEY (question_id) REFERENCES questions(id) ON DELETE CASCADE;

-- Question bank rows belong to an exam.
ALTER TABLE questions
    DROP CONSTRAINT IF EXISTS questions_exam_id_fkey;

ALTER TABLE questions
    ADD CONSTRAINT questions_exam_id_fkey
    FOREIGN KEY (exam_id) REFERENCES exams(id) ON DELETE CASCADE;

-- Legacy selected-exam pointer on users.
ALTER TABLE users
    DROP CONSTRAINT IF EXISTS fk_users_selected_exam;

ALTER TABLE users
    DROP CONSTRAINT IF EXISTS users_selected_exam_id_fkey;

ALTER TABLE users
    ADD CONSTRAINT fk_users_selected_exam
    FOREIGN KEY (selected_exam_id) REFERENCES exams(id) ON DELETE SET NULL;

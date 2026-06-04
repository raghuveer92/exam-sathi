-- Exam catalog: categories, merchandising fields, student enrollment metadata

CREATE TABLE IF NOT EXISTS exam_categories (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(100) NOT NULL UNIQUE,
    description VARCHAR(500),
    icon VARCHAR(80),
    display_order INT NOT NULL DEFAULT 0,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP
);

ALTER TABLE exams ADD COLUMN IF NOT EXISTS category_id BIGINT REFERENCES exam_categories(id);
ALTER TABLE exams ADD COLUMN IF NOT EXISTS short_description VARCHAR(200);
ALTER TABLE exams ADD COLUMN IF NOT EXISTS banner_url VARCHAR(500);
ALTER TABLE exams ADD COLUMN IF NOT EXISTS difficulty_level VARCHAR(30);
ALTER TABLE exams ADD COLUMN IF NOT EXISTS featured BOOLEAN NOT NULL DEFAULT FALSE;
ALTER TABLE exams ADD COLUMN IF NOT EXISTS popular BOOLEAN NOT NULL DEFAULT FALSE;
ALTER TABLE exams ADD COLUMN IF NOT EXISTS display_order INT NOT NULL DEFAULT 0;
ALTER TABLE exams ADD COLUMN IF NOT EXISTS featured_order INT NOT NULL DEFAULT 0;

ALTER TABLE user_exam ADD COLUMN IF NOT EXISTS experience_level VARCHAR(30);

CREATE INDEX IF NOT EXISTS idx_exams_category_id ON exams(category_id);
CREATE INDEX IF NOT EXISTS idx_exams_featured ON exams(featured) WHERE featured = TRUE;
CREATE INDEX IF NOT EXISTS idx_exam_categories_display_order ON exam_categories(display_order);

-- ============================================================
-- Render DB: Delete CBSE 10th (id=7) & RBSE 10th (id=8)
-- then insert SSC CGL with all subjects, chapters, topics.
-- ============================================================

BEGIN;

-- ============================================================
-- PART 1: DELETE CBSE 10th + RBSE 10th (cascade-safe order)
-- ============================================================

-- 1a. Delete study_progress for affected topics
DELETE FROM study_progress
WHERE topic_id IN (
    SELECT t.id FROM topics t
    JOIN chapters c ON c.id = t.chapter_id
    JOIN subjects s ON s.id = c.subject_id
    WHERE s.exam_id IN (7, 8)
);

-- 1b. Detach users from these exams (avoid FK violation)
UPDATE users SET selected_exam_id = NULL WHERE selected_exam_id IN (7, 8);

-- 1c. Delete topics → chapters → subjects → exams
DELETE FROM topics
WHERE chapter_id IN (
    SELECT c.id FROM chapters c
    JOIN subjects s ON s.id = c.subject_id
    WHERE s.exam_id IN (7, 8)
);

DELETE FROM chapters
WHERE subject_id IN (SELECT id FROM subjects WHERE exam_id IN (7, 8));

DELETE FROM subjects WHERE exam_id IN (7, 8);

DELETE FROM exams WHERE id IN (7, 8);

-- ============================================================
-- PART 2: INSERT SSC CGL exam + data
-- ============================================================
DO $$
DECLARE
    exam_id       BIGINT;

    english_id    BIGINT;
    gk_id         BIGINT;
    reasoning_id  BIGINT;
    math_id       BIGINT;

    english_ch    BIGINT;
    gk_ch         BIGINT;
    reasoning_ch  BIGINT;
    math_ch       BIGINT;
BEGIN

    -- --------------------------------------------------------
    -- 2a. Exam
    -- --------------------------------------------------------
    INSERT INTO exams (name, code, description, color_code, icon_url,
                       is_active, created_at, updated_at)
    VALUES ('SSC CGL', 'SSC_CGL',
            'Staff Selection Commission Combined Graduate Level',
            '#2563EB', 'workspace_premium',
            true, NOW(), NOW())
    RETURNING id INTO exam_id;

    -- --------------------------------------------------------
    -- 2b. Subjects
    -- --------------------------------------------------------
    INSERT INTO subjects (name, icon_name, color_code, display_order, is_active,
                          description, created_at, updated_at, exam_id)
    VALUES ('English', 'menu_book', '#2563EB', 1, true,
            'English Language and Comprehension', NOW(), NOW(), exam_id)
    RETURNING id INTO english_id;

    INSERT INTO subjects (name, icon_name, color_code, display_order, is_active,
                          description, created_at, updated_at, exam_id)
    VALUES ('General Knowledge', 'public', '#10B981', 2, true,
            'General Awareness and Current Affairs', NOW(), NOW(), exam_id)
    RETURNING id INTO gk_id;

    INSERT INTO subjects (name, icon_name, color_code, display_order, is_active,
                          description, created_at, updated_at, exam_id)
    VALUES ('Reasoning', 'psychology', '#F59E0B', 3, true,
            'General Intelligence and Reasoning', NOW(), NOW(), exam_id)
    RETURNING id INTO reasoning_id;

    INSERT INTO subjects (name, icon_name, color_code, display_order, is_active,
                          description, created_at, updated_at, exam_id)
    VALUES ('Mathematics', 'calculate', '#EF4444', 4, true,
            'Quantitative Aptitude', NOW(), NOW(), exam_id)
    RETURNING id INTO math_id;

    -- --------------------------------------------------------
    -- 2c. One chapter per subject
    -- --------------------------------------------------------
    INSERT INTO chapters (title, order_index, is_active, created_at, updated_at, subject_id)
    VALUES ('English Topics', 1, true, NOW(), NOW(), english_id)
    RETURNING id INTO english_ch;

    INSERT INTO chapters (title, order_index, is_active, created_at, updated_at, subject_id)
    VALUES ('General Knowledge Topics', 1, true, NOW(), NOW(), gk_id)
    RETURNING id INTO gk_ch;

    INSERT INTO chapters (title, order_index, is_active, created_at, updated_at, subject_id)
    VALUES ('Reasoning Topics', 1, true, NOW(), NOW(), reasoning_id)
    RETURNING id INTO reasoning_ch;

    INSERT INTO chapters (title, order_index, is_active, created_at, updated_at, subject_id)
    VALUES ('Mathematics Topics', 1, true, NOW(), NOW(), math_id)
    RETURNING id INTO math_ch;

    -- --------------------------------------------------------
    -- 2d. English Topics  (12 × 10h = 120h)
    -- --------------------------------------------------------
    INSERT INTO topics (title, order_index, difficulty_level, estimated_hours,
                        is_active, created_at, updated_at, chapter_id)
    VALUES
        ('Active Passive',          1,  'MEDIUM', 10, true, NOW(), NOW(), english_ch),
        ('Cloze Test',              2,  'MEDIUM', 10, true, NOW(), NOW(), english_ch),
        ('Error Spotting',          3,  'MEDIUM', 10, true, NOW(), NOW(), english_ch),
        ('Fill in the Blanks',      4,  'EASY',   10, true, NOW(), NOW(), english_ch),
        ('Idioms and Phrases',      5,  'MEDIUM', 10, true, NOW(), NOW(), english_ch),
        ('One Word Substitution',   6,  'MEDIUM', 10, true, NOW(), NOW(), english_ch),
        ('Reading Comprehension',   7,  'HARD',   10, true, NOW(), NOW(), english_ch),
        ('Sentence Correction',     8,  'MEDIUM', 10, true, NOW(), NOW(), english_ch),
        ('Sentence Improvement',    9,  'MEDIUM', 10, true, NOW(), NOW(), english_ch),
        ('Sentence Rearrangement',  10, 'HARD',   10, true, NOW(), NOW(), english_ch),
        ('Spellings Correction',    11, 'EASY',   10, true, NOW(), NOW(), english_ch),
        ('Synonyms-Antonyms',       12, 'MEDIUM', 10, true, NOW(), NOW(), english_ch);

    -- --------------------------------------------------------
    -- 2e. General Knowledge Topics  (15 × 12h = 180h)
    -- --------------------------------------------------------
    INSERT INTO topics (title, order_index, difficulty_level, estimated_hours,
                        is_active, created_at, updated_at, chapter_id)
    VALUES
        ('Books and Authors',                    1,  'EASY',   12, true, NOW(), NOW(), gk_ch),
        ('Current Affairs',                      2,  'MEDIUM', 12, true, NOW(), NOW(), gk_ch),
        ('Important Days',                       3,  'EASY',   12, true, NOW(), NOW(), gk_ch),
        ('Important Schemes',                    4,  'MEDIUM', 12, true, NOW(), NOW(), gk_ch),
        ('India and its Neighbouring Countries', 5,  'MEDIUM', 12, true, NOW(), NOW(), gk_ch),
        ('History',                              6,  'MEDIUM', 12, true, NOW(), NOW(), gk_ch),
        ('Culture',                              7,  'EASY',   12, true, NOW(), NOW(), gk_ch),
        ('Geography',                            8,  'MEDIUM', 12, true, NOW(), NOW(), gk_ch),
        ('Polity',                               9,  'MEDIUM', 12, true, NOW(), NOW(), gk_ch),
        ('Scientific Research',                  10, 'HARD',   12, true, NOW(), NOW(), gk_ch),
        ('People in News',                       11, 'EASY',   12, true, NOW(), NOW(), gk_ch),
        ('Portfolio',                            12, 'EASY',   12, true, NOW(), NOW(), gk_ch),
        ('Science',                              13, 'MEDIUM', 12, true, NOW(), NOW(), gk_ch),
        ('Sports',                               14, 'EASY',   12, true, NOW(), NOW(), gk_ch),
        ('Static GK',                            15, 'MEDIUM', 12, true, NOW(), NOW(), gk_ch);

    -- --------------------------------------------------------
    -- 2f. Reasoning Topics  (20 × 7h = 140h)
    -- --------------------------------------------------------
    INSERT INTO topics (title, order_index, difficulty_level, estimated_hours,
                        is_active, created_at, updated_at, chapter_id)
    VALUES
        ('Analogies',                    1,  'EASY',   7, true, NOW(), NOW(), reasoning_ch),
        ('Analysis',                     2,  'MEDIUM', 7, true, NOW(), NOW(), reasoning_ch),
        ('Arithmetic Number Series',     3,  'MEDIUM', 7, true, NOW(), NOW(), reasoning_ch),
        ('Arithmetical Reasoning',       4,  'MEDIUM', 7, true, NOW(), NOW(), reasoning_ch),
        ('Blood Relations',              5,  'MEDIUM', 7, true, NOW(), NOW(), reasoning_ch),
        ('Coding and Decoding',          6,  'MEDIUM', 7, true, NOW(), NOW(), reasoning_ch),
        ('Decision Making',              7,  'HARD',   7, true, NOW(), NOW(), reasoning_ch),
        ('Discrimination',               8,  'EASY',   7, true, NOW(), NOW(), reasoning_ch),
        ('Figural Classification',       9,  'MEDIUM', 7, true, NOW(), NOW(), reasoning_ch),
        ('Judgment',                     10, 'MEDIUM', 7, true, NOW(), NOW(), reasoning_ch),
        ('Non-verbal Series',            11, 'MEDIUM', 7, true, NOW(), NOW(), reasoning_ch),
        ('Observation',                  12, 'EASY',   7, true, NOW(), NOW(), reasoning_ch),
        ('Problem-solving',              13, 'HARD',   7, true, NOW(), NOW(), reasoning_ch),
        ('Relationship Concepts',        14, 'EASY',   7, true, NOW(), NOW(), reasoning_ch),
        ('Similarities and Differences', 15, 'EASY',   7, true, NOW(), NOW(), reasoning_ch),
        ('Space Visualization',          16, 'HARD',   7, true, NOW(), NOW(), reasoning_ch),
        ('Spatial Orientation',          17, 'HARD',   7, true, NOW(), NOW(), reasoning_ch),
        ('Statement Conclusion',         18, 'MEDIUM', 7, true, NOW(), NOW(), reasoning_ch),
        ('Syllogistic Reasoning',        19, 'HARD',   7, true, NOW(), NOW(), reasoning_ch),
        ('Visual Memory',                20, 'MEDIUM', 7, true, NOW(), NOW(), reasoning_ch);

    -- --------------------------------------------------------
    -- 2g. Mathematics Topics  (30×6h + 8×5h = 220h)
    -- --------------------------------------------------------
    INSERT INTO topics (title, order_index, difficulty_level, estimated_hours,
                        is_active, created_at, updated_at, chapter_id)
    VALUES
        ('Averages',                                                                            1,  'EASY',   6, true, NOW(), NOW(), math_ch),
        ('Bar Diagram & Pie Chart',                                                             2,  'EASY',   5, true, NOW(), NOW(), math_ch),
        ('Basic Algebraic Identities of School Algebra & Elementary Surds',                    3,  'MEDIUM', 6, true, NOW(), NOW(), math_ch),
        ('Circle and its Chords, Tangents, Angles Subtended by Chords of a Circle, Common Tangents to Two or More Circles',
                                                                                                4,  'HARD',   6, true, NOW(), NOW(), math_ch),
        ('Complementary Angles',                                                                5,  'EASY',   5, true, NOW(), NOW(), math_ch),
        ('Computation of Whole Numbers',                                                        6,  'EASY',   5, true, NOW(), NOW(), math_ch),
        ('Congruence and Similarity of Triangles',                                             7,  'MEDIUM', 6, true, NOW(), NOW(), math_ch),
        ('Decimals',                                                                            8,  'EASY',   5, true, NOW(), NOW(), math_ch),
        ('Degree and Radian Measures',                                                          9,  'MEDIUM', 6, true, NOW(), NOW(), math_ch),
        ('Discount',                                                                            10, 'MEDIUM', 6, true, NOW(), NOW(), math_ch),
        ('Fractions',                                                                           11, 'EASY',   5, true, NOW(), NOW(), math_ch),
        ('Frequency Polygon',                                                                   12, 'EASY',   5, true, NOW(), NOW(), math_ch),
        ('Graphs of Linear Equations',                                                          13, 'MEDIUM', 6, true, NOW(), NOW(), math_ch),
        ('Heights and Distances',                                                               14, 'HARD',   6, true, NOW(), NOW(), math_ch),
        ('Hemispheres',                                                                         15, 'MEDIUM', 6, true, NOW(), NOW(), math_ch),
        ('Histogram',                                                                           16, 'EASY',   5, true, NOW(), NOW(), math_ch),
        ('Interest',                                                                            17, 'MEDIUM', 6, true, NOW(), NOW(), math_ch),
        ('Mixture and Alligation',                                                              18, 'HARD',   6, true, NOW(), NOW(), math_ch),
        ('Partnership Business',                                                                19, 'MEDIUM', 6, true, NOW(), NOW(), math_ch),
        ('Percentage',                                                                          20, 'MEDIUM', 6, true, NOW(), NOW(), math_ch),
        ('Profit and Loss',                                                                     21, 'MEDIUM', 6, true, NOW(), NOW(), math_ch),
        ('Quadrilaterals',                                                                      22, 'MEDIUM', 6, true, NOW(), NOW(), math_ch),
        ('Ratio & Proportion',                                                                  23, 'EASY',   6, true, NOW(), NOW(), math_ch),
        ('Rectangular Parallelepiped',                                                          24, 'HARD',   6, true, NOW(), NOW(), math_ch),
        ('Regular Polygons',                                                                    25, 'MEDIUM', 6, true, NOW(), NOW(), math_ch),
        ('Regular Right Pyramid with Triangular or Square Base',                               26, 'HARD',   6, true, NOW(), NOW(), math_ch),
        ('Relationships Between Numbers',                                                       27, 'EASY',   6, true, NOW(), NOW(), math_ch),
        ('Right Circular Cone',                                                                 28, 'MEDIUM', 6, true, NOW(), NOW(), math_ch),
        ('Right Circular Cylinder',                                                             29, 'MEDIUM', 6, true, NOW(), NOW(), math_ch),
        ('Right Prism',                                                                         30, 'MEDIUM', 6, true, NOW(), NOW(), math_ch),
        ('Sphere',                                                                              31, 'MEDIUM', 6, true, NOW(), NOW(), math_ch),
        ('Square Roots',                                                                        32, 'EASY',   5, true, NOW(), NOW(), math_ch),
        ('Standard Identities',                                                                 33, 'MEDIUM', 6, true, NOW(), NOW(), math_ch),
        ('Time & Work',                                                                         34, 'MEDIUM', 6, true, NOW(), NOW(), math_ch),
        ('Time and Distance',                                                                   35, 'MEDIUM', 6, true, NOW(), NOW(), math_ch),
        ('Triangle',                                                                            36, 'MEDIUM', 6, true, NOW(), NOW(), math_ch),
        ('Triangle and its Various Kinds of Centres',                                          37, 'HARD',   6, true, NOW(), NOW(), math_ch),
        ('Trigonometric Ratio',                                                                 38, 'HARD',   6, true, NOW(), NOW(), math_ch);

END $$;

COMMIT;

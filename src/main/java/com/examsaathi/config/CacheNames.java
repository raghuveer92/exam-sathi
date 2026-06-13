package com.examsaathi.config;

/**
 * Central registry of Spring Cache names and their intended TTLs (see {@link CacheConfig}).
 */
public final class CacheNames {

    private CacheNames() {}

    /** Default TTL: 30 minutes */
    public static final String EXAMS = "exams";
    public static final String EXAM_LIST = "examList";
    public static final String SUBJECTS_BY_EXAM = "subjectsByExam";
    public static final String SUBJECTS = "subjects";
    public static final String CHAPTERS_BY_SUBJECT = "chaptersBySubject";
    public static final String CHAPTERS = "chapters";
    public static final String TOPICS_BY_CHAPTER = "topicsByChapter";
    public static final String TOPICS_BY_EXAM = "topicsByExam";
    public static final String TOPICS = "topics";
    public static final String EXAM_CATALOG = "examCatalog";
    public static final String EXAM_CATEGORIES = "examCategories";
    public static final String SYNC_CATALOG = "syncCatalog";
    public static final String MOCK_TEST_INFO = "mockTestInfo";
    public static final String TOPIC_TEST_CONFIG = "topicTestConfig";
    public static final String EXAM_SUBJECT_GROUPS = "examSubjectGroups";

    /** TTL: 5 minutes */
    public static final String DASHBOARD = "dashboard";
    public static final String LEADERBOARD = "leaderboard";
    public static final String ANALYTICS = "analytics";

    /** TTL: 24 hours — reserved for future current-affairs API */
    public static final String CURRENT_AFFAIRS = "currentAffairs";

    public static final String[] CATALOG_CACHES = {
        EXAMS, EXAM_LIST, SUBJECTS_BY_EXAM, SUBJECTS, CHAPTERS_BY_SUBJECT, CHAPTERS,
        TOPICS_BY_CHAPTER, TOPICS_BY_EXAM, TOPICS, EXAM_CATALOG, EXAM_CATEGORIES,
        SYNC_CATALOG, MOCK_TEST_INFO, TOPIC_TEST_CONFIG, EXAM_SUBJECT_GROUPS, CURRENT_AFFAIRS
    };

    public static final String[] ALL_CACHES = {
        EXAMS, EXAM_LIST, SUBJECTS_BY_EXAM, SUBJECTS, CHAPTERS_BY_SUBJECT, CHAPTERS,
        TOPICS_BY_CHAPTER, TOPICS_BY_EXAM, TOPICS, EXAM_CATALOG, EXAM_CATEGORIES,
        SYNC_CATALOG, MOCK_TEST_INFO, TOPIC_TEST_CONFIG, EXAM_SUBJECT_GROUPS,
        DASHBOARD, LEADERBOARD, ANALYTICS, CURRENT_AFFAIRS
    };
}

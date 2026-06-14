package com.examsaathi.service;

import com.examsaathi.config.CacheNames;
import com.examsaathi.config.GoogleSheetsProperties;
import com.examsaathi.dto.sheet.SheetQuestion;
import com.examsaathi.entity.Exam;
import com.examsaathi.entity.Subject;
import com.examsaathi.entity.Topic;
import com.examsaathi.exception.GoogleSheetsException;
import com.examsaathi.util.MockTestMasteryUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class GoogleSheetsService {

    private final GoogleSheetsQuestionLoader loader;
    private final GoogleSheetsProperties properties;
    private final CacheManager cacheManager;

    public List<SheetQuestion> loadQuestions(String sheetId) {
        try {
            return loader.loadQuestions(sheetId);
        } catch (Exception ex) {
            if (ex instanceof GoogleSheetsException) {
                throw ex;
            }
            throw new GoogleSheetsException(
                GoogleSheetsException.ErrorCode.CACHE_FAILURE,
                "Failed to load cached sheet questions",
                ex);
        }
    }

    @CacheEvict(value = CacheNames.SHEET_QUESTIONS, key = "#sheetId")
    public void evictCache(String sheetId) {
        // annotation-driven eviction
    }

    public List<SheetQuestion> refreshQuestions(String sheetId) {
        evictCache(sheetId);
        try {
            List<SheetQuestion> fresh = loader.fetchAndParse(sheetId);
            Cache cache = cacheManager.getCache(CacheNames.SHEET_QUESTIONS);
            if (cache != null) {
                cache.put(sheetId, fresh);
            }
            return fresh;
        } catch (Exception ex) {
            if (ex instanceof GoogleSheetsException) {
                throw ex;
            }
            throw new GoogleSheetsException(
                GoogleSheetsException.ErrorCode.CACHE_FAILURE,
                "Failed to refresh sheet question cache",
                ex);
        }
    }

    public String resolveSheetId(Exam exam) {
        return GoogleSheetsQuestionLoader.resolveSheetId(exam.getSheetId(), exam.getSheetUrl());
    }

    public String requireSheetId(Exam exam) {
        String sheetId = resolveSheetId(exam);
        if (sheetId == null || sheetId.isBlank()) {
            throw new GoogleSheetsException(
                GoogleSheetsException.ErrorCode.SHEET_NOT_CONFIGURED,
                "Exam does not have a Google Sheet configured");
        }
        return sheetId;
    }

    public boolean hasSheetConfigured(Exam exam) {
        return resolveSheetId(exam) != null;
    }

    public int getMinQuestionsPerTopic() {
        return properties.getMinQuestionsPerTopic();
    }

    /**
     * For sheet-backed tests, use up to configured count but never require more than exist in the pool.
     */
    public int resolveEffectiveQuestionCount(int configuredNum, int available, boolean sheetBacked) {
        if (!sheetBacked || available <= 0) {
            return configuredNum;
        }
        return Math.max(
            properties.getMinQuestionsPerTopic(),
            Math.min(configuredNum, available));
    }

    public boolean canStartTest(int configuredNum, int available, boolean sheetBacked, boolean isActive) {
        if (!isActive || available <= 0) {
            return false;
        }
        int required = resolveEffectiveQuestionCount(configuredNum, available, sheetBacked);
        return available >= required;
    }

    public List<SheetQuestion> filterForTopic(List<SheetQuestion> all, Exam exam, Subject subject, Topic topic) {
        boolean idColumnsPresent = all.stream().anyMatch(q ->
            q.getTopicId() != null && !q.getTopicId().isBlank());

        if (idColumnsPresent) {
            List<SheetQuestion> byId = all.stream()
                .filter(q -> matchesTopicId(q, exam.getId(), subject.getId(), topic.getId()))
                .toList();
            if (!byId.isEmpty()) {
                return byId;
            }
        }

        return all.stream()
            .filter(q -> MockTestMasteryUtil.labelsMatch(q.getExam(), exam.getName()))
            .filter(q -> MockTestMasteryUtil.labelsMatch(q.getSubject(), subject.getName()))
            .filter(q -> MockTestMasteryUtil.labelsMatch(q.getTopic(), topic.getTitle()))
            .toList();
    }

    public boolean matchesTopic(SheetQuestion q, Exam exam, Subject subject, Topic topic) {
        if (q.getTopicId() != null && !q.getTopicId().isBlank()) {
            return matchesTopicId(q, exam.getId(), subject.getId(), topic.getId());
        }
        return MockTestMasteryUtil.labelsMatch(q.getExam(), exam.getName())
            && MockTestMasteryUtil.labelsMatch(q.getSubject(), subject.getName())
            && MockTestMasteryUtil.labelsMatch(q.getTopic(), topic.getTitle());
    }

    private boolean matchesTopicId(SheetQuestion q, Long examId, Long subjectId, Long topicId) {
        if (q.getTopicId() == null || q.getTopicId().isBlank()) {
            return false;
        }
        if (!String.valueOf(topicId).equals(q.getTopicId().trim())) {
            return false;
        }
        if (q.getSubjectId() != null && !q.getSubjectId().isBlank()
            && !String.valueOf(subjectId).equals(q.getSubjectId().trim())) {
            return false;
        }
        if (q.getExamId() != null && !q.getExamId().isBlank()
            && !String.valueOf(examId).equals(q.getExamId().trim())) {
            return false;
        }
        return true;
    }

    public List<SheetQuestion> applyDifficultyFilter(List<SheetQuestion> pool, String filter) {
        if (filter == null || filter.isBlank() || "ALL".equalsIgnoreCase(filter)) {
            return pool;
        }
        String want = filter.trim().toUpperCase();
        return pool.stream()
            .filter(q -> q.getDifficulty() != null && q.getDifficulty().trim().toUpperCase().equals(want))
            .toList();
    }

    public void validatePoolSize(List<SheetQuestion> pool, int required, String context) {
        int min = Math.max(required, properties.getMinQuestionsPerTopic());
        if (pool.isEmpty()) {
            throw new GoogleSheetsException(
                GoogleSheetsException.ErrorCode.TOPIC_EMPTY,
                "No sheet questions found for " + context);
        }
        if (pool.size() < min) {
            throw new GoogleSheetsException(
                GoogleSheetsException.ErrorCode.INSUFFICIENT_QUESTIONS,
                "Not enough sheet questions for " + context
                    + ". Required: " + min + ", available: " + pool.size());
        }
    }

    public String describeMatchingStrategy(List<SheetQuestion> questions) {
        boolean hasIds = questions.stream().anyMatch(q ->
            q.getTopicId() != null && !q.getTopicId().isBlank());
        return hasIds ? "ID_PRIMARY_NAME_FALLBACK" : "NAME_ONLY";
    }

    public Optional<SheetQuestion> findSheetTopicKey(List<SheetQuestion> questions, SheetQuestion sample) {
        return questions.stream()
            .filter(q -> sample.getTopic() != null && MockTestMasteryUtil.labelsMatch(q.getTopic(), sample.getTopic()))
            .findFirst();
    }

    public List<String> collectUnmappedSheetTopics(List<SheetQuestion> all, List<Topic> dbTopics, Exam exam) {
        List<String> unmapped = new ArrayList<>();
        for (SheetQuestion q : all) {
            if (!MockTestMasteryUtil.labelsMatch(q.getExam(), exam.getName())) {
                continue;
            }
            boolean matched = dbTopics.stream().anyMatch(t ->
                matchesTopic(q, exam, t.getChapter().getSubject(), t));
            if (!matched && q.getTopic() != null) {
                String label = q.getSubject() + " / " + q.getTopic();
                if (!unmapped.contains(label)) {
                    unmapped.add(label);
                }
            }
        }
        return unmapped;
    }
}

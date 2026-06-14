package com.examsaathi.service;

import com.examsaathi.config.GoogleSheetsProperties;
import com.examsaathi.dto.response.*;
import com.examsaathi.dto.sheet.SheetQuestion;
import com.examsaathi.entity.Exam;
import com.examsaathi.entity.Topic;
import com.examsaathi.entity.TopicTestConfig;
import com.examsaathi.exception.ResourceNotFoundException;
import com.examsaathi.repository.ExamRepository;
import com.examsaathi.repository.TopicRepository;
import com.examsaathi.repository.TopicTestConfigRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class GoogleSheetsAdminService {

    private final ExamRepository examRepository;
    private final TopicRepository topicRepository;
    private final TopicTestConfigRepository topicTestConfigRepository;
    private final GoogleSheetsService googleSheetsService;
    private final GoogleSheetsQuestionLoader questionLoader;
    private final GoogleSheetsProperties properties;

    public SheetValidationResponse validateExamSheet(Long examId) {
        Exam exam = examRepository.findById(examId)
            .orElseThrow(() -> new ResourceNotFoundException("Exam", examId));

        String sheetId = googleSheetsService.requireSheetId(exam);
        List<SheetQuestion> questions = questionLoader.fetchAndParse(sheetId);

        return buildValidationReport(exam, sheetId, questions);
    }

    @Transactional
    public SheetCacheRefreshResponse refreshExamSheetCache(Long examId) {
        Exam exam = examRepository.findById(examId)
            .orElseThrow(() -> new ResourceNotFoundException("Exam", examId));
        String sheetId = googleSheetsService.requireSheetId(exam);
        List<SheetQuestion> refreshed = googleSheetsService.refreshQuestions(sheetId);
        return SheetCacheRefreshResponse.builder()
            .examId(examId)
            .sheetId(sheetId)
            .questionCount(refreshed.size())
            .refreshed(true)
            .build();
    }

    @Transactional
    public SheetCacheRefreshResponse refreshSheetCacheBySheetId(String sheetId) {
        List<SheetQuestion> refreshed = googleSheetsService.refreshQuestions(sheetId);
        return SheetCacheRefreshResponse.builder()
            .sheetId(sheetId)
            .questionCount(refreshed.size())
            .refreshed(true)
            .build();
    }

    private SheetValidationResponse buildValidationReport(Exam exam, String sheetId, List<SheetQuestion> questions) {
        List<String> structureErrors = new ArrayList<>();
        List<SheetRowIssueResponse> rowIssues = new ArrayList<>();
        int validRows = 0;
        int invalidRows = 0;

        Set<String> seenIds = new HashSet<>();
        for (int i = 0; i < questions.size(); i++) {
            SheetQuestion q = questions.get(i);
            List<String> issues = validateQuestionRow(q, seenIds);
            if (issues.isEmpty()) {
                validRows++;
            } else {
                invalidRows++;
                for (String issue : issues) {
                    rowIssues.add(SheetRowIssueResponse.builder()
                        .rowNumber(i + 2)
                        .questionId(q.getId())
                        .issue(issue)
                        .build());
                }
            }
        }

        if (questions.isEmpty()) {
            structureErrors.add("No valid question rows found in sheet");
        }

        boolean usesIdMatching = questions.stream()
            .anyMatch(q -> q.getTopicId() != null && !q.getTopicId().isBlank());

        List<Topic> examTopics = topicRepository.findByExamIdWithHierarchy(exam.getId());
        List<TopicSheetMappingResponse> topicMappings = new ArrayList<>();
        List<String> dbTopicsWithoutQuestions = new ArrayList<>();

        for (Topic topic : examTopics) {
            List<SheetQuestion> topicPool = googleSheetsService.filterForTopic(
                questions, exam, topic.getChapter().getSubject(), topic);
            int required = topicTestConfigRepository.findByTopicId(topic.getId())
                .map(TopicTestConfig::getNumQuestions)
                .orElse(10);
            int effectiveRequired = googleSheetsService.resolveEffectiveQuestionCount(
                required, topicPool.size(), true);
            boolean matchedById = usesIdMatching && topicPool.stream()
                .anyMatch(q -> q.getTopicId() != null && q.getTopicId().equals(String.valueOf(topic.getId())));
            boolean matchedByName = topicPool.stream()
                .anyMatch(q -> q.getTopic() != null
                    && q.getTopic().equalsIgnoreCase(topic.getTitle()));

            topicMappings.add(TopicSheetMappingResponse.builder()
                .topicId(topic.getId())
                .topicTitle(topic.getTitle())
                .subjectId(topic.getChapter().getSubject().getId())
                .subjectName(topic.getChapter().getSubject().getName())
                .sheetQuestionCount(topicPool.size())
                .matchedById(matchedById)
                .matchedByName(matchedByName)
                .sufficientForTest(topicPool.size() >= effectiveRequired)
                .requiredQuestionCount(effectiveRequired)
                .build());

            if (topicPool.isEmpty()) {
                dbTopicsWithoutQuestions.add(
                    topic.getChapter().getSubject().getName() + " / " + topic.getTitle());
            }
        }

        List<String> sheetTopicsWithoutDb = googleSheetsService.collectUnmappedSheetTopics(
            questions, examTopics, exam);

        boolean valid = structureErrors.isEmpty()
            && invalidRows == 0
            && dbTopicsWithoutQuestions.isEmpty()
            && sheetTopicsWithoutDb.isEmpty();

        return SheetValidationResponse.builder()
            .examId(exam.getId())
            .examName(exam.getName())
            .sheetId(sheetId)
            .valid(valid)
            .totalRows(questions.size())
            .validQuestionRows(validRows)
            .invalidQuestionRows(invalidRows)
            .missingColumns(List.of())
            .structureErrors(structureErrors)
            .rowIssues(rowIssues.stream().limit(50).toList())
            .topicMappings(topicMappings)
            .sheetTopicsWithoutDbMatch(sheetTopicsWithoutDb)
            .dbTopicsWithoutSheetQuestions(dbTopicsWithoutQuestions)
            .usesIdBasedMatching(usesIdMatching)
            .matchingStrategy(googleSheetsService.describeMatchingStrategy(questions))
            .build();
    }

    private List<String> validateQuestionRow(SheetQuestion q, Set<String> seenIds) {
        List<String> issues = new ArrayList<>();
        if (q.getId() != null && seenIds.contains(q.getId())) {
            issues.add("Duplicate question id: " + q.getId());
        } else if (q.getId() != null) {
            seenIds.add(q.getId());
        }
        if (q.getQuestionEn() == null || q.getQuestionEn().isBlank()) {
            issues.add("Missing question_en");
        }
        if (!GoogleSheetsQuestionLoader.isValidCorrectOption(q.getCorrectOption())) {
            issues.add("Invalid or missing correct_option (must be A, B, C, or D)");
        }
        if (q.getExam() == null || q.getExam().isBlank()) {
            issues.add("Missing exam label");
        }
        if (q.getSubject() == null || q.getSubject().isBlank()) {
            issues.add("Missing subject label");
        }
        if (q.getTopic() == null || q.getTopic().isBlank()) {
            issues.add("Missing topic label");
        }
        return issues;
    }

    private int resolveRequiredCount(Long topicId) {
        return topicTestConfigRepository.findByTopicId(topicId)
            .map(c -> c.getNumQuestions())
            .orElse(10);
    }
}

package com.examsaathi.service;

import com.examsaathi.config.CacheNames;
import com.examsaathi.config.GoogleSheetsProperties;
import com.examsaathi.dto.sheet.SheetQuestion;
import com.examsaathi.exception.GoogleSheetsException;
import com.examsaathi.util.CsvLineParser;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import jakarta.annotation.PostConstruct;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Cached fetch layer for Google Sheet CSV export.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class GoogleSheetsQuestionLoader {

    private static final Pattern SHEET_ID_PATTERN =
        Pattern.compile("/spreadsheets/d/([a-zA-Z0-9-_]+)");
    private static final Set<String> VALID_OPTIONS = Set.of("A", "B", "C", "D");
    private static final Set<String> VALID_DIFFICULTIES = Set.of("EASY", "MEDIUM", "HARD");

    private final GoogleSheetsProperties properties;
    private RestClient restClient;

    @PostConstruct
    void initRestClient() {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(properties.getConnectTimeoutMs());
        factory.setReadTimeout(properties.getReadTimeoutMs());
        restClient = RestClient.builder().requestFactory(factory).build();
    }

    @Cacheable(value = CacheNames.SHEET_QUESTIONS, key = "#sheetId", unless = "#result == null")
    public List<SheetQuestion> loadQuestions(String sheetId) {
        return fetchAndParse(sheetId);
    }

    public List<SheetQuestion> fetchAndParse(String sheetId) {
        if (sheetId == null || sheetId.isBlank()) {
            throw new GoogleSheetsException(
                GoogleSheetsException.ErrorCode.SHEET_NOT_CONFIGURED,
                "Exam sheet_id is not configured");
        }

        String url = "https://docs.google.com/spreadsheets/d/" + sheetId.trim() + "/export?format=csv";
        try {
            String csv = restClient.get()
                .uri(url)
                .retrieve()
                .body(String.class);

            if (csv == null || csv.isBlank()) {
                throw new GoogleSheetsException(
                    GoogleSheetsException.ErrorCode.SHEET_EMPTY,
                    "Google Sheet returned empty data for id: " + sheetId);
            }

            return parseCsv(csv);
        } catch (GoogleSheetsException ex) {
            throw ex;
        } catch (RestClientException ex) {
            log.warn("Failed to fetch Google Sheet {}: {}", sheetId, ex.getMessage());
            throw new GoogleSheetsException(
                GoogleSheetsException.ErrorCode.SHEET_ACCESS_FAILED,
                "Unable to fetch Google Sheet. Check sheet id, sharing settings, and network connectivity.",
                ex);
        } catch (Exception ex) {
            log.error("Unexpected error loading sheet {}", sheetId, ex);
            throw new GoogleSheetsException(
                GoogleSheetsException.ErrorCode.SHEET_ACCESS_FAILED,
                "Failed to load Google Sheet data",
                ex);
        }
    }

    public static String extractSheetIdFromUrl(String sheetUrl) {
        if (sheetUrl == null || sheetUrl.isBlank()) {
            return null;
        }
        Matcher matcher = SHEET_ID_PATTERN.matcher(sheetUrl);
        return matcher.find() ? matcher.group(1) : null;
    }

    public static String resolveSheetId(String sheetId, String sheetUrl) {
        if (sheetId != null && !sheetId.isBlank()) {
            return sheetId.trim();
        }
        return extractSheetIdFromUrl(sheetUrl);
    }

    List<SheetQuestion> parseCsv(String csv) {
        String[] lines = csv.split("\\r?\\n");
        if (lines.length < 2) {
            throw new GoogleSheetsException(
                GoogleSheetsException.ErrorCode.SHEET_INVALID_FORMAT,
                "Sheet must contain a header row and at least one data row");
        }

        List<String> headers = CsvLineParser.parseLine(lines[0]);
        validateRequiredColumns(headers);

        List<SheetQuestion> questions = new ArrayList<>();
        for (int i = 1; i < lines.length; i++) {
            String line = lines[i];
            if (line.isBlank()) {
                continue;
            }
            List<String> cols = CsvLineParser.parseLine(line);
            String id = getColumn(headers, cols, "id");
            if (id == null || id.isBlank()) {
                continue;
            }

            SheetQuestion question = SheetQuestion.builder()
                .id(id.trim())
                .exam(getColumn(headers, cols, "exam"))
                .subject(getColumn(headers, cols, "subject"))
                .topic(getColumn(headers, cols, "topic"))
                .examId(getColumn(headers, cols, "exam_id"))
                .subjectId(getColumn(headers, cols, "subject_id"))
                .topicId(getColumn(headers, cols, "topic_id"))
                .questionEn(getColumn(headers, cols, "question_en"))
                .questionHi(getColumn(headers, cols, "question_hi"))
                .optionAEn(getColumn(headers, cols, "optionA_en"))
                .optionAHi(getColumn(headers, cols, "optionA_hi"))
                .optionBEn(getColumn(headers, cols, "optionB_en"))
                .optionBHi(getColumn(headers, cols, "optionB_hi"))
                .optionCEn(getColumn(headers, cols, "optionC_en"))
                .optionCHi(getColumn(headers, cols, "optionC_hi"))
                .optionDEn(getColumn(headers, cols, "optionD_en"))
                .optionDHi(getColumn(headers, cols, "optionD_hi"))
                .correctOption(normalizeUpper(getColumn(headers, cols, "correct_option")))
                .difficulty(normalizeUpper(getColumn(headers, cols, "difficulty")))
                .explanationEn(getColumn(headers, cols, "explanation_en"))
                .explanationHi(getColumn(headers, cols, "explanation_hi"))
                .tags(getColumn(headers, cols, "tags"))
                .build();
            validateQuestionRow(question, i + 1);
            questions.add(question);
        }

        log.info("Parsed {} questions from Google Sheet", questions.size());
        return questions;
    }

    private void validateRequiredColumns(List<String> headers) {
        List<String> missing = new ArrayList<>();
        for (String required : properties.getRequiredColumns()) {
            boolean found = headers.stream()
                .anyMatch(h -> h.trim().equalsIgnoreCase(required));
            if (!found) {
                missing.add(required);
            }
        }
        if (!missing.isEmpty()) {
            throw new GoogleSheetsException(
                GoogleSheetsException.ErrorCode.SHEET_INVALID_FORMAT,
                "Sheet missing required columns: " + String.join(", ", missing));
        }
    }

    static boolean isValidCorrectOption(String option) {
        if (option == null || option.isBlank()) {
            return false;
        }
        return VALID_OPTIONS.contains(option.trim().toUpperCase());
    }

    private static void validateQuestionRow(SheetQuestion question, int rowNumber) {
        List<String> missing = new ArrayList<>();
        requireValue(question.getExam(), "exam", missing);
        requireValue(question.getSubject(), "subject", missing);
        requireValue(question.getTopic(), "topic", missing);
        requireValue(question.getQuestionEn(), "question_en", missing);
        requireValue(question.getQuestionHi(), "question_hi", missing);
        requireValue(question.getOptionAEn(), "optionA_en", missing);
        requireValue(question.getOptionAHi(), "optionA_hi", missing);
        requireValue(question.getOptionBEn(), "optionB_en", missing);
        requireValue(question.getOptionBHi(), "optionB_hi", missing);
        requireValue(question.getOptionCEn(), "optionC_en", missing);
        requireValue(question.getOptionCHi(), "optionC_hi", missing);
        requireValue(question.getOptionDEn(), "optionD_en", missing);
        requireValue(question.getOptionDHi(), "optionD_hi", missing);
        requireValue(question.getExplanationEn(), "explanation_en", missing);
        requireValue(question.getExplanationHi(), "explanation_hi", missing);
        requireValue(question.getTags(), "tags", missing);

        if (!missing.isEmpty()) {
            throw new GoogleSheetsException(
                GoogleSheetsException.ErrorCode.SHEET_INVALID_FORMAT,
                "Sheet row " + rowNumber + " missing required values: " + String.join(", ", missing));
        }
        if (!isValidCorrectOption(question.getCorrectOption())) {
            throw new GoogleSheetsException(
                GoogleSheetsException.ErrorCode.SHEET_INVALID_FORMAT,
                "Sheet row " + rowNumber + " has invalid correct_option: " + question.getCorrectOption());
        }
        if (question.getDifficulty() == null
            || !VALID_DIFFICULTIES.contains(question.getDifficulty().trim().toUpperCase())) {
            throw new GoogleSheetsException(
                GoogleSheetsException.ErrorCode.SHEET_INVALID_FORMAT,
                "Sheet row " + rowNumber + " has invalid difficulty: " + question.getDifficulty());
        }
    }

    private static void requireValue(String value, String column, List<String> missing) {
        if (value == null || value.isBlank()) {
            missing.add(column);
        }
    }

    private static String normalizeUpper(String value) {
        return value == null ? null : value.trim().toUpperCase();
    }

    private static String getColumn(List<String> headers, List<String> cols, String name) {
        int index = -1;
        for (int i = 0; i < headers.size(); i++) {
            if (headers.get(i).trim().equalsIgnoreCase(name)) {
                index = i;
                break;
            }
        }
        if (index < 0 || index >= cols.size()) {
            return null;
        }
        return cols.get(index);
    }
}

package com.examsaathi.util;

import com.examsaathi.dto.request.QuestionOptionRequest;
import com.examsaathi.dto.request.QuestionRequest;
import com.examsaathi.entity.Question;
import com.examsaathi.entity.Topic;
import com.examsaathi.exception.BadRequestException;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public final class QuestionTextFormatParser {

    private static final Pattern FIELD_LINE = Pattern.compile("^([A-Z][A-Z0-9_]*):\\s*(.*)$");

    private QuestionTextFormatParser() {}

    public static List<ParsedQuestion> parse(String text) {
        if (text == null || text.isBlank()) {
            throw new BadRequestException("Question text is empty");
        }

        String normalized = text.replace("\r\n", "\n").trim();
        String[] blocks = normalized.split("\\n---\\n");
        List<ParsedQuestion> results = new ArrayList<>();

        for (int i = 0; i < blocks.length; i++) {
            String block = blocks[i].trim();
            if (block.isBlank()) {
                continue;
            }
            try {
                results.add(parseBlock(block));
            } catch (BadRequestException ex) {
                throw new BadRequestException("Question " + (i + 1) + ": " + ex.getMessage());
            }
        }

        if (results.isEmpty()) {
            throw new BadRequestException("No questions found in text");
        }
        return results;
    }

    /** Parses replace payload; blank or separator-only content clears all questions. */
    public static List<ParsedQuestion> parseReplaceContent(String text) {
        if (text == null || text.isBlank()) {
            return List.of();
        }

        String normalized = text.replace("\r\n", "\n").trim();
        if (isSeparatorOnlyContent(normalized)) {
            return List.of();
        }

        String[] blocks = normalized.split("\\n---\\n");
        for (String block : blocks) {
            String trimmed = block.trim();
            if (!trimmed.isBlank() && !isSeparatorOnlyContent(trimmed)) {
                return parse(text);
            }
        }
        return List.of();
    }

    private static boolean isSeparatorOnlyContent(String text) {
        for (String line : text.split("\n")) {
            String trimmed = line.trim();
            if (trimmed.isEmpty()) {
                continue;
            }
            if (!trimmed.chars().allMatch(ch -> ch == '-')) {
                return false;
            }
        }
        return true;
    }

    private static ParsedQuestion parseBlock(String block) {
        Map<String, StringBuilder> values = new LinkedHashMap<>();
        String currentKey = null;

        for (String rawLine : block.split("\n", -1)) {
            String trimmed = rawLine.trim();
            if (trimmed.isBlank()) {
                if (currentKey != null) {
                    StringBuilder sb = values.get(currentKey);
                    if (!sb.isEmpty()) {
                        sb.append('\n');
                    }
                }
                continue;
            }

            Matcher fieldMatcher = FIELD_LINE.matcher(trimmed);
            if (fieldMatcher.matches()) {
                currentKey = fieldMatcher.group(1);
                values.computeIfAbsent(currentKey, key -> new StringBuilder())
                    .append(fieldMatcher.group(2));
            } else if (currentKey != null) {
                StringBuilder sb = values.get(currentKey);
                if (!sb.isEmpty()) {
                    sb.append('\n');
                }
                sb.append(trimmed);
            } else {
                throw new BadRequestException("Expected field starting with KEY:, got: " + trimmed);
            }
        }

        Map<String, String> fields = new LinkedHashMap<>();
        Map<String, String> options = new LinkedHashMap<>();
        for (var entry : values.entrySet()) {
            String key = entry.getKey();
            String value = entry.getValue().toString().trim();
            if (key.startsWith("OPTION_") && key.length() == "OPTION_".length() + 1) {
                options.put(key.substring("OPTION_".length()), value);
            } else {
                fields.put(key, value);
            }
        }

        validate(fields, options);
        return new ParsedQuestion(fields, options);
    }

    private static void validate(Map<String, String> fields, Map<String, String> options) {
        if (!fields.containsKey("QUESTION") || fields.get("QUESTION").isBlank()) {
            throw new BadRequestException("QUESTION is required");
        }
        if (!fields.containsKey("TYPE") || fields.get("TYPE").isBlank()) {
            throw new BadRequestException("TYPE is required");
        }
        if (options.size() < 2) {
            throw new BadRequestException("At least two OPTION_X fields are required");
        }
        if (!fields.containsKey("CORRECT") || fields.get("CORRECT").isBlank()) {
            throw new BadRequestException("CORRECT is required");
        }

        double marks = parseDoubleField(fields.get("MARKS"), "MARKS", 1.0);
        if (marks <= 0) {
            throw new BadRequestException("MARKS must be greater than 0");
        }
        double negative = parseDoubleField(fields.get("NEGATIVE_MARKS"), "NEGATIVE_MARKS", 0.0);
        if (negative < 0) {
            throw new BadRequestException("NEGATIVE_MARKS cannot be negative");
        }

        Set<String> correctKeys = parseCorrectKeys(fields.get("CORRECT"));
        for (String key : correctKeys) {
            if (!options.containsKey(key)) {
                throw new BadRequestException("CORRECT references unknown option: " + key);
            }
        }

        Question.QuestionType type = parseType(fields.get("TYPE"));
        if (type == Question.QuestionType.SINGLE_CORRECT && correctKeys.size() != 1) {
            throw new BadRequestException("SINGLE_CORRECT requires exactly one CORRECT option");
        }
        if (type == Question.QuestionType.MULTIPLE_CORRECT && correctKeys.isEmpty()) {
            throw new BadRequestException("MULTIPLE_CORRECT requires at least one CORRECT option");
        }
    }

    public static QuestionRequest toRequest(ParsedQuestion parsed, Topic topic, Long examId) {
        Map<String, String> fields = parsed.fields();
        Map<String, String> options = parsed.options();
        Set<String> correctKeys = parseCorrectKeys(fields.get("CORRECT"));
        Question.QuestionType type = parseType(fields.get("TYPE"));

        List<QuestionOptionRequest> optionRequests = new ArrayList<>();
        List<String> sortedKeys = new ArrayList<>(options.keySet());
        Collections.sort(sortedKeys);
        int order = 0;
        for (String key : sortedKeys) {
            QuestionOptionRequest opt = new QuestionOptionRequest();
            opt.setOptionKey(key);
            opt.setOptionText(options.get(key));
            opt.setIsCorrect(correctKeys.contains(key));
            opt.setDisplayOrder(order++);
            optionRequests.add(opt);
        }

        QuestionRequest request = new QuestionRequest();
        request.setExamId(examId);
        request.setSubjectId(topic.getChapter().getSubject().getId());
        request.setChapterId(topic.getChapter().getId());
        request.setTopicId(topic.getId());
        request.setQuestionText(fields.get("QUESTION"));
        request.setQuestionType(type.name());
        request.setExplanation(blankToNull(fields.get("EXPLANATION")));
        request.setMarks(parseDoubleField(fields.get("MARKS"), "MARKS", 1.0));
        request.setNegativeMarks(parseDoubleField(fields.get("NEGATIVE_MARKS"), "NEGATIVE_MARKS", 0.0));
        request.setPreviousYear(parseBoolean(fields.get("PREVIOUS_YEAR")));
        request.setPreviousYearValue(blankToNull(fields.get("PREVIOUS_YEAR_VALUE")));
        request.setIsActive(true);
        request.setOptions(optionRequests);
        return request;
    }

    private static Question.QuestionType parseType(String raw) {
        String normalized = raw.trim().toUpperCase().replace(' ', '_');
        if (normalized.contains("MULTIPLE")) {
            return Question.QuestionType.MULTIPLE_CORRECT;
        }
        return Question.QuestionType.SINGLE_CORRECT;
    }

    private static Set<String> parseCorrectKeys(String raw) {
        return Arrays.stream(raw.split("[|,]"))
            .map(String::trim)
            .filter(s -> !s.isBlank())
            .map(String::toUpperCase)
            .collect(Collectors.toCollection(LinkedHashSet::new));
    }

    private static double parseDoubleField(String raw, String label, double defaultValue) {
        if (raw == null || raw.isBlank()) {
            return defaultValue;
        }
        try {
            return Double.parseDouble(raw.trim());
        } catch (NumberFormatException ex) {
            throw new BadRequestException(label + " must be a number");
        }
    }

    private static boolean parseBoolean(String raw) {
        if (raw == null || raw.isBlank()) {
            return false;
        }
        String value = raw.trim();
        return value.equalsIgnoreCase("true") || value.equals("1") || value.equalsIgnoreCase("yes");
    }

    private static String blankToNull(String raw) {
        return raw == null || raw.isBlank() ? null : raw.trim();
    }

    public record ParsedQuestion(Map<String, String> fields, Map<String, String> options) {}
}

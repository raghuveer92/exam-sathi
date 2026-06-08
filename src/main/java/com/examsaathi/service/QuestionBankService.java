package com.examsaathi.service;

import com.examsaathi.dto.request.BulkQuestionImportRequest;
import com.examsaathi.dto.request.QuestionOptionRequest;
import com.examsaathi.dto.request.QuestionRequest;
import com.examsaathi.dto.response.BulkQuestionImportResponse;
import com.examsaathi.dto.response.QuestionOptionResponse;
import com.examsaathi.dto.response.QuestionResponse;
import com.examsaathi.entity.*;
import com.examsaathi.exception.BadRequestException;
import com.examsaathi.exception.ResourceNotFoundException;
import com.examsaathi.repository.*;
import com.examsaathi.util.QuestionTextFormatParser;
import com.examsaathi.util.QuestionTextFormatParser.ParsedQuestion;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class QuestionBankService {

    private final QuestionRepository questionRepository;
    private final TestAttemptAnswerRepository testAttemptAnswerRepository;
    private final ExamRepository examRepository;
    private final SubjectRepository subjectRepository;
    private final ChapterRepository chapterRepository;
    private final TopicRepository topicRepository;

    @Transactional(readOnly = true)
    public List<QuestionResponse> listQuestions(Long topicId, Long chapterId, Long subjectId, Long examId) {
        List<Question> questions;
        if (topicId != null) {
            questions = questionRepository.findByTopicIdOrderByIdAsc(topicId);
        } else if (chapterId != null) {
            questions = questionRepository.findAll().stream()
                .filter(q -> q.getChapter().getId().equals(chapterId))
                .sorted(Comparator.comparing(Question::getId))
                .toList();
        } else if (subjectId != null) {
            questions = questionRepository.findAll().stream()
                .filter(q -> q.getSubject().getId().equals(subjectId))
                .sorted(Comparator.comparing(Question::getId))
                .toList();
        } else if (examId != null) {
            questions = questionRepository.findAll().stream()
                .filter(q -> q.getExam().getId().equals(examId))
                .sorted(Comparator.comparing(Question::getId))
                .toList();
        } else {
            questions = questionRepository.findAll();
        }
        return questions.stream().map(this::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public QuestionResponse getById(Long id) {
        return toResponse(findQuestion(id));
    }

    public QuestionResponse create(QuestionRequest request) {
        Question question = buildQuestion(new Question(), request);
        validateOptions(question);
        return toResponse(questionRepository.save(question));
    }

    public QuestionResponse update(Long id, QuestionRequest request) {
        Question question = findQuestion(id);
        question.getOptions().clear();
        buildQuestion(question, request);
        validateOptions(question);
        return toResponse(questionRepository.save(question));
    }

    public void delete(Long id) {
        if (!questionRepository.existsById(id)) {
            throw new ResourceNotFoundException("Question", id);
        }
        if (testAttemptAnswerRepository.existsByQuestionId(id)) {
            throw new BadRequestException(
                "Question cannot be deleted because it is used in test attempts. Deactivate it instead.");
        }
        questionRepository.deleteById(id);
    }

    public QuestionResponse updateStatus(Long id, boolean isActive) {
        Question question = findQuestion(id);
        question.setIsActive(isActive);
        return toResponse(questionRepository.save(question));
    }

    public BulkQuestionImportResponse replaceQuestionsForTopic(
            Long topicId, Long examId, BulkQuestionImportRequest request) {
        Topic topic = topicRepository.findByIdWithChapterAndSubject(topicId)
            .orElseThrow(() -> new ResourceNotFoundException("Topic", topicId));
        if (!examRepository.existsById(examId)) {
            throw new ResourceNotFoundException("Exam", examId);
        }
        List<ParsedQuestion> parsedQuestions =
            QuestionTextFormatParser.parseReplaceContent(request.getTextContent());
        clearActiveQuestionsForTopic(topicId);
        if (parsedQuestions.isEmpty()) {
            return BulkQuestionImportResponse.builder()
                .totalRows(0)
                .imported(0)
                .failed(0)
                .errors(List.of())
                .build();
        }
        return importParsedQuestions(topic, examId, parsedQuestions);
    }

    public void deleteAllQuestionsForTopic(Long topicId) {
        if (!topicRepository.existsById(topicId)) {
            throw new ResourceNotFoundException("Topic", topicId);
        }
        testAttemptAnswerRepository.deleteByTopicId(topicId);
        for (Question question : questionRepository.findByTopicIdOrderByIdAsc(topicId)) {
            questionRepository.delete(question);
        }
    }

    public void clearActiveQuestionsForTopic(Long topicId) {
        deleteAllQuestionsForTopic(topicId);
    }

    private BulkQuestionImportResponse importParsedQuestions(
            Topic topic, Long examId, List<ParsedQuestion> parsedQuestions) {
        List<String> errors = new ArrayList<>();
        int imported = 0;

        for (int i = 0; i < parsedQuestions.size(); i++) {
            try {
                QuestionRequest req = QuestionTextFormatParser.toRequest(parsedQuestions.get(i), topic, examId);
                create(req);
                imported++;
            } catch (Exception ex) {
                errors.add("Question " + (i + 1) + ": " + ex.getMessage());
            }
        }

        return BulkQuestionImportResponse.builder()
            .totalRows(parsedQuestions.size())
            .imported(imported)
            .failed(errors.size())
            .errors(errors)
            .build();
    }

    private Question buildQuestion(Question question, QuestionRequest request) {
        question.setExam(examRepository.findById(request.getExamId())
            .orElseThrow(() -> new ResourceNotFoundException("Exam", request.getExamId())));
        question.setSubject(subjectRepository.findById(request.getSubjectId())
            .orElseThrow(() -> new ResourceNotFoundException("Subject", request.getSubjectId())));
        question.setChapter(chapterRepository.findById(request.getChapterId())
            .orElseThrow(() -> new ResourceNotFoundException("Chapter", request.getChapterId())));
        question.setTopic(topicRepository.findById(request.getTopicId())
            .orElseThrow(() -> new ResourceNotFoundException("Topic", request.getTopicId())));
        question.setQuestionText(request.getQuestionText().trim());
        question.setQuestionType(parseQuestionTypeEnum(request.getQuestionType()));
        question.setExplanation(request.getExplanation());
        question.setMarks(request.getMarks() != null ? request.getMarks() : 1.0);
        question.setNegativeMarks(request.getNegativeMarks() != null ? request.getNegativeMarks() : 0.0);
        if (request.getDifficultyLevel() != null) {
            question.setDifficultyLevel(Topic.DifficultyLevel.valueOf(request.getDifficultyLevel().toUpperCase()));
        }
        question.setPreviousYear(Boolean.TRUE.equals(request.getPreviousYear()));
        question.setPreviousYearValue(request.getPreviousYearValue());
        if (request.getIsActive() != null) {
            question.setIsActive(request.getIsActive());
        }

        int order = 0;
        for (QuestionOptionRequest optReq : request.getOptions()) {
            QuestionOption option = QuestionOption.builder()
                .question(question)
                .optionKey(optReq.getOptionKey().trim().toUpperCase())
                .optionText(optReq.getOptionText().trim())
                .isCorrect(Boolean.TRUE.equals(optReq.getIsCorrect()))
                .displayOrder(optReq.getDisplayOrder() != null ? optReq.getDisplayOrder() : order++)
                .build();
            question.getOptions().add(option);
        }
        return question;
    }

    private void validateOptions(Question question) {
        long correctCount = question.getOptions().stream().filter(QuestionOption::getIsCorrect).count();
        if (correctCount == 0) {
            throw new BadRequestException("At least one correct option is required");
        }
        if (question.getQuestionType() == Question.QuestionType.SINGLE_CORRECT && correctCount != 1) {
            throw new BadRequestException("Single correct questions must have exactly one correct option");
        }
    }

    private Question findQuestion(Long id) {
        return questionRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Question", id));
    }

    private QuestionResponse toResponse(Question q) {
        return QuestionResponse.builder()
            .id(q.getId())
            .examId(q.getExam().getId())
            .examName(q.getExam().getName())
            .subjectId(q.getSubject().getId())
            .subjectName(q.getSubject().getName())
            .chapterId(q.getChapter().getId())
            .chapterTitle(q.getChapter().getTitle())
            .topicId(q.getTopic().getId())
            .topicTitle(q.getTopic().getTitle())
            .questionText(q.getQuestionText())
            .questionType(q.getQuestionType().name())
            .explanation(q.getExplanation())
            .marks(q.getMarks())
            .negativeMarks(q.getNegativeMarks())
            .difficultyLevel(q.getDifficultyLevel().name())
            .previousYear(q.getPreviousYear())
            .previousYearValue(q.getPreviousYearValue())
            .isActive(q.getIsActive())
            .options(q.getOptions().stream().map(o -> QuestionOptionResponse.builder()
                .id(o.getId())
                .optionKey(o.getOptionKey())
                .optionText(o.getOptionText())
                .isCorrect(o.getIsCorrect())
                .displayOrder(o.getDisplayOrder())
                .build()).toList())
            .createdAt(q.getCreatedAt())
            .build();
    }

    private static Question.QuestionType parseQuestionTypeEnum(String raw) {
        String normalized = raw.trim().toUpperCase().replace(' ', '_');
        if (normalized.contains("MULTIPLE")) {
            return Question.QuestionType.MULTIPLE_CORRECT;
        }
        return Question.QuestionType.SINGLE_CORRECT;
    }
}

package com.examsaathi.controller;

import com.examsaathi.dto.request.BulkQuestionImportRequest;
import com.examsaathi.dto.request.QuestionRequest;
import com.examsaathi.dto.response.ApiResponse;
import com.examsaathi.dto.response.BulkQuestionImportResponse;
import com.examsaathi.dto.response.QuestionResponse;
import com.examsaathi.service.QuestionBankService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/admin/questions")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Question Bank", description = "Admin question bank management")
public class QuestionBankController {

    private final QuestionBankService questionBankService;

    @GetMapping
    @Operation(summary = "List questions with optional filters")
    public ResponseEntity<ApiResponse<List<QuestionResponse>>> list(
            @RequestParam(required = false) Long examId,
            @RequestParam(required = false) Long subjectId,
            @RequestParam(required = false) Long chapterId,
            @RequestParam(required = false) Long topicId) {
        return ResponseEntity.ok(ApiResponse.success(
            questionBankService.listQuestions(topicId, chapterId, subjectId, examId)));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<QuestionResponse>> get(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success(questionBankService.getById(id)));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<QuestionResponse>> create(@Valid @RequestBody QuestionRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(ApiResponse.success("Question created", questionBankService.create(request)));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<QuestionResponse>> update(
            @PathVariable Long id,
            @Valid @RequestBody QuestionRequest request) {
        return ResponseEntity.ok(ApiResponse.success("Question updated", questionBankService.update(id, request)));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable Long id) {
        questionBankService.delete(id);
        return ResponseEntity.ok(ApiResponse.success("Question deleted", null));
    }

    @PatchMapping("/{id}/status")
    public ResponseEntity<ApiResponse<QuestionResponse>> updateStatus(
            @PathVariable Long id,
            @RequestParam Boolean isActive) {
        return ResponseEntity.ok(ApiResponse.success(
            "Question status updated", questionBankService.updateStatus(id, isActive)));
    }

    @PostMapping("/topic/{topicId}/replace")
    @Operation(summary = "Replace all questions for a topic from text format")
    public ResponseEntity<ApiResponse<BulkQuestionImportResponse>> replaceForTopic(
            @PathVariable Long topicId,
            @RequestParam Long examId,
            @Valid @RequestBody BulkQuestionImportRequest request) {
        return ResponseEntity.ok(ApiResponse.success(
            questionBankService.replaceQuestionsForTopic(topicId, examId, request)));
    }
}

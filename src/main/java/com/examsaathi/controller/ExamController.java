package com.examsaathi.controller;

import com.examsaathi.dto.request.ExamRequest;
import com.examsaathi.dto.response.ApiResponse;
import com.examsaathi.dto.response.ExamResponse;
import com.examsaathi.service.ExamService;
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
@RequestMapping("/exams")
@RequiredArgsConstructor
@Tag(name = "Exams", description = "Exam management APIs")
public class ExamController {

    private final ExamService examService;

    @GetMapping
    @Operation(summary = "Get all active exams (public)")
    public ResponseEntity<ApiResponse<List<ExamResponse>>> getAllExams() {
        return ResponseEntity.ok(ApiResponse.success(examService.getAllActiveExams()));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get exam by ID with subjects (public)")
    public ResponseEntity<ApiResponse<ExamResponse>> getExam(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success(examService.getExamById(id)));
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "Create exam (Admin only)")
    public ResponseEntity<ApiResponse<ExamResponse>> createExam(
            @Valid @RequestBody ExamRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(ApiResponse.success("Exam created", examService.createExam(request)));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "Update exam (Admin only)")
    public ResponseEntity<ApiResponse<ExamResponse>> updateExam(
            @PathVariable Long id,
            @Valid @RequestBody ExamRequest request) {
        return ResponseEntity.ok(ApiResponse.success("Exam updated", examService.updateExam(id, request)));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "Delete exam (Admin only)")
    public ResponseEntity<ApiResponse<Void>> deleteExam(@PathVariable Long id) {
        examService.deleteExam(id);
        return ResponseEntity.ok(ApiResponse.success("Exam deleted", null));
    }
}

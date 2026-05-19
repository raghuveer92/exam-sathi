package com.examsaathi.controller;

import com.examsaathi.dto.request.SubjectRequest;
import com.examsaathi.dto.response.ApiResponse;
import com.examsaathi.dto.response.SubjectResponse;
import com.examsaathi.service.SubjectService;
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
@RequestMapping("/subjects")
@RequiredArgsConstructor
@Tag(name = "Subjects", description = "Subject management with dynamic color and icon")
public class SubjectController {

    private final SubjectService subjectService;

    @GetMapping("/exam/{examId}")
    @Operation(summary = "Get subjects by exam (public)")
    public ResponseEntity<ApiResponse<List<SubjectResponse>>> getSubjects(
            @PathVariable Long examId) {
        return ResponseEntity.ok(ApiResponse.success(subjectService.getSubjectsByExam(examId)));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get subject by ID with chapters (public)")
    public ResponseEntity<ApiResponse<SubjectResponse>> getSubject(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success(subjectService.getSubjectById(id)));
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "Create subject with color & icon (Admin)")
    public ResponseEntity<ApiResponse<SubjectResponse>> createSubject(
            @Valid @RequestBody SubjectRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(ApiResponse.success("Subject created", subjectService.createSubject(request)));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "Update subject color, icon, order (Admin)")
    public ResponseEntity<ApiResponse<SubjectResponse>> updateSubject(
            @PathVariable Long id,
            @Valid @RequestBody SubjectRequest request) {
        return ResponseEntity.ok(ApiResponse.success("Subject updated", subjectService.updateSubject(id, request)));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "Delete subject (Admin)")
    public ResponseEntity<ApiResponse<Void>> deleteSubject(@PathVariable Long id) {
        subjectService.deleteSubject(id);
        return ResponseEntity.ok(ApiResponse.success("Subject deleted", null));
    }
}

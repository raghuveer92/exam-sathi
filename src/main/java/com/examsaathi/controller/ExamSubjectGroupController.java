package com.examsaathi.controller;

import com.examsaathi.dto.request.ExamSubjectGroupRequest;
import com.examsaathi.dto.response.ApiResponse;
import com.examsaathi.dto.response.ExamSubjectGroupResponse;
import com.examsaathi.service.ExamSubjectGroupService;
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
@RequestMapping("/subject-groups")
@RequiredArgsConstructor
@Tag(name = "Subject Groups", description = "Exam subject group management APIs")
public class ExamSubjectGroupController {

    private final ExamSubjectGroupService examSubjectGroupService;

    @GetMapping("/exam/{examId}")
    @Operation(summary = "Get subject groups by exam")
    public ResponseEntity<ApiResponse<List<ExamSubjectGroupResponse>>> getGroupsByExam(@PathVariable Long examId) {
        return ResponseEntity.ok(ApiResponse.success(examSubjectGroupService.getGroupsByExam(examId)));
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "Create a subject group")
    public ResponseEntity<ApiResponse<ExamSubjectGroupResponse>> createGroup(
            @Valid @RequestBody ExamSubjectGroupRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(ApiResponse.success("Subject group created", examSubjectGroupService.createGroup(request)));
    }

    @PutMapping("/{groupId}")
    @PreAuthorize("hasRole('ADMIN')")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "Update a subject group")
    public ResponseEntity<ApiResponse<ExamSubjectGroupResponse>> updateGroup(
            @PathVariable Long groupId,
            @Valid @RequestBody ExamSubjectGroupRequest request) {
        return ResponseEntity.ok(ApiResponse.success("Subject group updated", examSubjectGroupService.updateGroup(groupId, request)));
    }

    @DeleteMapping("/{groupId}")
    @PreAuthorize("hasRole('ADMIN')")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "Delete a subject group")
    public ResponseEntity<ApiResponse<Void>> deleteGroup(@PathVariable Long groupId) {
        examSubjectGroupService.deleteGroup(groupId);
        return ResponseEntity.ok(ApiResponse.success("Subject group deleted", null));
    }
}
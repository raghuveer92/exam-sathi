package com.examsaathi.controller;

import com.examsaathi.dto.request.ExamCategoryRequest;
import com.examsaathi.dto.response.ApiResponse;
import com.examsaathi.dto.response.ExamCategoryResponse;
import com.examsaathi.service.ExamCategoryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/admin/exam-categories")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
@Tag(name = "Admin Exam Categories", description = "Manage exam catalog categories")
public class AdminExamCategoryController {

    private final ExamCategoryService categoryService;

    @GetMapping
    @Operation(summary = "List all exam categories")
    public ResponseEntity<ApiResponse<List<ExamCategoryResponse>>> list() {
        return ResponseEntity.ok(ApiResponse.success(categoryService.getAllCategories()));
    }

    @PostMapping
    @Operation(summary = "Create exam category")
    public ResponseEntity<ApiResponse<ExamCategoryResponse>> create(@Valid @RequestBody ExamCategoryRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(ApiResponse.success("Category created", categoryService.create(request)));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update exam category")
    public ResponseEntity<ApiResponse<ExamCategoryResponse>> update(
            @PathVariable Long id,
            @Valid @RequestBody ExamCategoryRequest request) {
        return ResponseEntity.ok(ApiResponse.success("Category updated", categoryService.update(id, request)));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete exam category")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable Long id) {
        categoryService.delete(id);
        return ResponseEntity.ok(ApiResponse.success("Category deleted", null));
    }
}

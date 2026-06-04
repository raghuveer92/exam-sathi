package com.examsaathi.controller;

import com.examsaathi.dto.response.ApiResponse;
import com.examsaathi.dto.response.ExamCatalogResponse;
import com.examsaathi.dto.response.ExamCategoryResponse;
import com.examsaathi.dto.response.ExamResponse;
import com.examsaathi.entity.User;
import com.examsaathi.repository.UserRepository;
import com.examsaathi.service.ExamCatalogService;
import com.examsaathi.service.ExamCategoryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/exam-catalog")
@RequiredArgsConstructor
@Tag(name = "Exam Catalog", description = "Categorized exam discovery for students")
public class ExamCatalogController {

    private final ExamCatalogService catalogService;
    private final ExamCategoryService categoryService;
    private final UserRepository userRepository;

    @GetMapping("/categories")
    @Operation(summary = "List active exam categories")
    public ResponseEntity<ApiResponse<List<ExamCategoryResponse>>> getCategories() {
        return ResponseEntity.ok(ApiResponse.success(categoryService.getActiveCategories()));
    }

    @GetMapping
    @Operation(summary = "Full catalog with categories, featured, and recommended exams")
    public ResponseEntity<ApiResponse<ExamCatalogResponse>> getCatalog(
            @AuthenticationPrincipal UserDetails userDetails) {
        Long userId = resolveUserId(userDetails);
        return ResponseEntity.ok(ApiResponse.success(catalogService.getCatalog(userId)));
    }

    @GetMapping("/categories/{categoryId}/exams")
    @Operation(summary = "Exams in a category")
    public ResponseEntity<ApiResponse<List<ExamResponse>>> getByCategory(@PathVariable Long categoryId) {
        return ResponseEntity.ok(ApiResponse.success(catalogService.getExamsByCategory(categoryId)));
    }

    @GetMapping("/featured")
    @Operation(summary = "Featured exams")
    public ResponseEntity<ApiResponse<List<ExamResponse>>> getFeatured() {
        return ResponseEntity.ok(ApiResponse.success(catalogService.getFeatured()));
    }

    @GetMapping("/search")
    @Operation(summary = "Search exams by name or description")
    public ResponseEntity<ApiResponse<List<ExamResponse>>> search(@RequestParam String q) {
        return ResponseEntity.ok(ApiResponse.success(catalogService.search(q)));
    }

    @GetMapping("/recommended")
    @Operation(summary = "Recommended exams for current student")
    public ResponseEntity<ApiResponse<List<ExamResponse>>> getRecommended(
            @AuthenticationPrincipal UserDetails userDetails) {
        Long userId = resolveUserId(userDetails);
        return ResponseEntity.ok(ApiResponse.success(catalogService.getRecommended(userId)));
    }

    private Long resolveUserId(UserDetails userDetails) {
        if (userDetails == null) return null;
        return userRepository.findByEmail(userDetails.getUsername())
            .map(User::getId)
            .orElse(null);
    }
}

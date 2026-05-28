package com.examsaathi.controller;

import com.examsaathi.dto.request.ChapterRequest;
import com.examsaathi.dto.request.BulkTopicRequest;
import com.examsaathi.dto.request.TopicRequest;
import com.examsaathi.dto.response.*;
import com.examsaathi.service.SyllabusService;
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
@RequestMapping("/syllabus")
@RequiredArgsConstructor
@Tag(name = "Syllabus", description = "Chapter and topic management APIs")
public class SyllabusController {

    private final SyllabusService syllabusService;

    // ===== Chapters =====

    @GetMapping("/chapters/subject/{subjectId}")
    @Operation(summary = "Get chapters by subject")
    public ResponseEntity<ApiResponse<List<ChapterResponse>>> getChapters(
            @PathVariable Long subjectId) {
        return ResponseEntity.ok(ApiResponse.success(syllabusService.getChaptersBySubject(subjectId)));
    }

    @GetMapping("/chapters/{id}")
    @Operation(summary = "Get chapter with topics")
    public ResponseEntity<ApiResponse<ChapterResponse>> getChapter(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success(syllabusService.getChapterById(id)));
    }

    @PostMapping("/chapters")
    @PreAuthorize("hasRole('ADMIN')")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "Create chapter (Admin)")
    public ResponseEntity<ApiResponse<ChapterResponse>> createChapter(
            @Valid @RequestBody ChapterRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(ApiResponse.success("Chapter created", syllabusService.createChapter(request)));
    }

    @PutMapping("/chapters/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "Update chapter (Admin)")
    public ResponseEntity<ApiResponse<ChapterResponse>> updateChapter(
            @PathVariable Long id,
            @Valid @RequestBody ChapterRequest request) {
        return ResponseEntity.ok(ApiResponse.success("Chapter updated", syllabusService.updateChapter(id, request)));
    }

    @DeleteMapping("/chapters/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "Delete chapter (Admin)")
    public ResponseEntity<ApiResponse<Void>> deleteChapter(@PathVariable Long id) {
        syllabusService.deleteChapter(id);
        return ResponseEntity.ok(ApiResponse.success("Chapter deleted", null));
    }

    // ===== Topics =====

    @GetMapping("/topics/chapter/{chapterId}")
    @Operation(summary = "Get topics by chapter")
    public ResponseEntity<ApiResponse<List<TopicResponse>>> getTopics(
            @PathVariable Long chapterId) {
        return ResponseEntity.ok(ApiResponse.success(syllabusService.getTopicsByChapter(chapterId)));
    }

    @GetMapping("/topics/{id}")
    @Operation(summary = "Get topic by ID")
    public ResponseEntity<ApiResponse<TopicResponse>> getTopic(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success(syllabusService.getTopicById(id)));
    }

    @PostMapping("/topics")
    @PreAuthorize("hasRole('ADMIN')")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "Create topic (Admin)")
    public ResponseEntity<ApiResponse<TopicResponse>> createTopic(
            @Valid @RequestBody TopicRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(ApiResponse.success("Topic created", syllabusService.createTopic(request)));
    }

    @PostMapping("/topics/bulk")
    @PreAuthorize("hasRole('ADMIN')")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "Create topics in bulk (Admin)")
    public ResponseEntity<ApiResponse<List<TopicResponse>>> createTopics(
            @Valid @RequestBody BulkTopicRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(ApiResponse.success("Topics created", syllabusService.createTopics(request.getTopics())));
    }

    @PutMapping("/topics/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "Update topic (Admin)")
    public ResponseEntity<ApiResponse<TopicResponse>> updateTopic(
            @PathVariable Long id,
            @Valid @RequestBody TopicRequest request) {
        return ResponseEntity.ok(ApiResponse.success("Topic updated", syllabusService.updateTopic(id, request)));
    }

    @DeleteMapping("/topics/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "Delete topic (Admin)")
    public ResponseEntity<ApiResponse<Void>> deleteTopic(@PathVariable Long id) {
        syllabusService.deleteTopic(id);
        return ResponseEntity.ok(ApiResponse.success("Topic deleted", null));
    }
}

package com.examsaathi.controller;

import com.examsaathi.dto.request.ProgressUpdateRequest;
import com.examsaathi.dto.request.StudyLogRequest;
import com.examsaathi.dto.response.*;
import com.examsaathi.entity.User;
import com.examsaathi.repository.UserRepository;
import com.examsaathi.service.StudyProgressService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/progress")
@RequiredArgsConstructor
@Tag(name = "Progress", description = "Study progress tracking APIs")
@SecurityRequirement(name = "bearerAuth")
public class ProgressController {

    private final StudyProgressService studyProgressService;
    private final UserRepository userRepository;

    @PostMapping("/topic")
    @Operation(summary = "Mark topic complete / update hours")
    public ResponseEntity<ApiResponse<Void>> updateTopicProgress(
            @AuthenticationPrincipal UserDetails userDetails,
            @Valid @RequestBody ProgressUpdateRequest request) {
        User user = userRepository.findByEmail(userDetails.getUsername()).orElseThrow();
        studyProgressService.updateProgress(user.getId(), request);
        return ResponseEntity.ok(ApiResponse.success("Progress updated", null));
    }

    @PostMapping("/log")
    @Operation(summary = "Log daily study hours")
    public ResponseEntity<ApiResponse<DailyStudyLogResponse>> logStudy(
            @AuthenticationPrincipal UserDetails userDetails,
            @Valid @RequestBody StudyLogRequest request) {
        User user = userRepository.findByEmail(userDetails.getUsername()).orElseThrow();
        return ResponseEntity.ok(ApiResponse.success(
            studyProgressService.logStudyHours(user.getId(), request)));
    }

    @GetMapping("/weekly")
    @Operation(summary = "Get last 7 days study logs")
    public ResponseEntity<ApiResponse<List<DailyStudyLogResponse>>> getWeeklyLogs(
            @AuthenticationPrincipal UserDetails userDetails) {
        User user = userRepository.findByEmail(userDetails.getUsername()).orElseThrow();
        return ResponseEntity.ok(ApiResponse.success(
            studyProgressService.getWeeklyLogs(user.getId())));
    }

    @GetMapping("/subjects/{examId}")
    @Operation(summary = "Get subject-wise progress for selected exam")
    public ResponseEntity<ApiResponse<List<SubjectProgressResponse>>> getSubjectProgress(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable Long examId) {
        User user = userRepository.findByEmail(userDetails.getUsername()).orElseThrow();
        return ResponseEntity.ok(ApiResponse.success(
            studyProgressService.getSubjectProgress(user.getId(), examId)));
    }
}

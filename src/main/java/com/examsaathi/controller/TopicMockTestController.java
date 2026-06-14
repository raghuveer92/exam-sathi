package com.examsaathi.controller;

import com.examsaathi.dto.request.SubmitTopicTestRequest;
import com.examsaathi.dto.response.ApiResponse;
import com.examsaathi.dto.response.TestAttemptResponse;
import com.examsaathi.dto.response.TopicMasteryResponse;
import com.examsaathi.repository.UserRepository;
import com.examsaathi.service.TopicMockTestService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/mock/topic")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Topic Mock Tests (Google Sheets)", description = "Sheet-backed topic mock tests")
public class TopicMockTestController {

    private final TopicMockTestService topicMockTestService;
    private final UserRepository userRepository;

    @GetMapping("/start")
    @Operation(summary = "Start a Google Sheets topic mock test")
    public ResponseEntity<ApiResponse<TestAttemptResponse>> start(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestParam Long topicId) {
        Long userId = currentUserId(userDetails);
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(ApiResponse.success("Test started",
                topicMockTestService.startTopicTest(userId, topicId)));
    }

    @PostMapping("/submit")
    @Operation(summary = "Submit a Google Sheets topic mock test")
    public ResponseEntity<ApiResponse<TestAttemptResponse>> submit(
            @AuthenticationPrincipal UserDetails userDetails,
            @Valid @RequestBody SubmitTopicTestRequest request) {
        return ResponseEntity.ok(ApiResponse.success(
            "Test submitted",
            topicMockTestService.submitTopicTest(currentUserId(userDetails), request)));
    }

    @GetMapping("/result")
    @Operation(summary = "Get result for a sheet-based topic mock test attempt")
    public ResponseEntity<ApiResponse<TestAttemptResponse>> result(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestParam Long attemptId) {
        return ResponseEntity.ok(ApiResponse.success(
            topicMockTestService.getTopicTestResult(currentUserId(userDetails), attemptId)));
    }

    @GetMapping("/mastery")
    @Operation(summary = "Get topic mastery and mock test availability")
    public ResponseEntity<ApiResponse<TopicMasteryResponse>> mastery(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestParam Long topicId) {
        return ResponseEntity.ok(ApiResponse.success(
            topicMockTestService.getTopicMastery(currentUserId(userDetails), topicId)));
    }

    private Long currentUserId(UserDetails userDetails) {
        return userRepository.findByEmail(userDetails.getUsername()).orElseThrow().getId();
    }
}

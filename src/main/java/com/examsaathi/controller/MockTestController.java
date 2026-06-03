package com.examsaathi.controller;

import com.examsaathi.dto.request.SubmitTestRequest;
import com.examsaathi.dto.response.ApiResponse;
import com.examsaathi.dto.response.TestAttemptResponse;
import com.examsaathi.dto.response.TestPerformanceResponse;
import com.examsaathi.dto.response.TopicTestConfigResponse;
import com.examsaathi.repository.UserRepository;
import com.examsaathi.service.MockTestService;
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

import java.util.List;

@RestController
@RequestMapping("/mock-tests")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Mock Tests", description = "Student topic-wise mock tests")
public class MockTestController {

    private final MockTestService mockTestService;
    private final UserRepository userRepository;

    @GetMapping("/topics/{topicId}/info")
    @Operation(summary = "Get topic test configuration and availability")
    public ResponseEntity<ApiResponse<TopicTestConfigResponse>> getTopicInfo(@PathVariable Long topicId) {
        return ResponseEntity.ok(ApiResponse.success(mockTestService.getTopicTestInfo(topicId)));
    }

    @PostMapping("/topics/{topicId}/start")
    @Operation(summary = "Start a new topic test attempt")
    public ResponseEntity<ApiResponse<TestAttemptResponse>> startTest(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable Long topicId) {
        Long userId = currentUserId(userDetails);
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(ApiResponse.success("Test started", mockTestService.startTest(userId, topicId)));
    }

    @GetMapping("/attempts/{attemptId}")
    public ResponseEntity<ApiResponse<TestAttemptResponse>> getAttempt(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable Long attemptId,
            @RequestParam(defaultValue = "false") boolean review) {
        return ResponseEntity.ok(ApiResponse.success(
            mockTestService.getAttempt(currentUserId(userDetails), attemptId, review)));
    }

    @GetMapping("/topics/{topicId}/attempts")
    public ResponseEntity<ApiResponse<List<TestAttemptResponse>>> listAttempts(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable Long topicId) {
        return ResponseEntity.ok(ApiResponse.success(
            mockTestService.getAttemptsForTopic(currentUserId(userDetails), topicId)));
    }

    @PostMapping("/attempts/{attemptId}/submit")
    public ResponseEntity<ApiResponse<TestAttemptResponse>> submit(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable Long attemptId,
            @Valid @RequestBody SubmitTestRequest request) {
        return ResponseEntity.ok(ApiResponse.success(
            "Test submitted",
            mockTestService.submitTest(currentUserId(userDetails), attemptId, request)));
    }

    @GetMapping("/performance")
    @Operation(summary = "Student mock test performance analytics")
    public ResponseEntity<ApiResponse<TestPerformanceResponse>> performance(
            @AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.ok(ApiResponse.success(
            mockTestService.getPerformance(currentUserId(userDetails))));
    }

    private Long currentUserId(UserDetails userDetails) {
        return userRepository.findByEmail(userDetails.getUsername()).orElseThrow().getId();
    }
}

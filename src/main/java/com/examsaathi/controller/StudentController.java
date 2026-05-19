package com.examsaathi.controller;

import com.examsaathi.dto.response.ApiResponse;
import com.examsaathi.dto.response.DashboardResponse;
import com.examsaathi.dto.response.UserResponse;
import com.examsaathi.entity.User;
import com.examsaathi.repository.UserRepository;
import com.examsaathi.service.DashboardService;
import com.examsaathi.service.UserMapper;
import jakarta.transaction.Transactional;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/student")
@RequiredArgsConstructor
@Tag(name = "Student", description = "Student profile and dashboard APIs")
@SecurityRequirement(name = "bearerAuth")
public class StudentController {

    private final UserRepository userRepository;
    private final DashboardService dashboardService;
    private final UserMapper userMapper;

    @GetMapping("/me")
    @Transactional
    @Operation(summary = "Get current student profile")
    public ResponseEntity<ApiResponse<UserResponse>> getProfile(
            @AuthenticationPrincipal UserDetails userDetails) {
        User user = userRepository.findByEmailWithExam(userDetails.getUsername()).orElseThrow();
        return ResponseEntity.ok(ApiResponse.success(userMapper.toResponse(user)));
    }

    @GetMapping("/dashboard")
    @Operation(summary = "Get student dashboard with full progress data")
    public ResponseEntity<ApiResponse<DashboardResponse>> getDashboard(
            @AuthenticationPrincipal UserDetails userDetails) {
        User user = userRepository.findByEmail(userDetails.getUsername()).orElseThrow();
        return ResponseEntity.ok(ApiResponse.success(dashboardService.getDashboard(user.getId())));
    }

    @PatchMapping("/exam/{examId}")
    @Transactional
    @Operation(summary = "Select exam to prepare for")
    public ResponseEntity<ApiResponse<UserResponse>> selectExam(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable Long examId) {
        User user = userRepository.findByEmailWithExam(userDetails.getUsername()).orElseThrow();
        com.examsaathi.entity.Exam exam = new com.examsaathi.entity.Exam();
        exam.setId(examId);
        user.setSelectedExam(exam);
        userRepository.save(user);
        return ResponseEntity.ok(ApiResponse.success("Exam selected", userMapper.toResponse(user)));
    }
}

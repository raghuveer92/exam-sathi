package com.examsaathi.controller;

import com.examsaathi.dto.response.AdminAnalyticsResponse;
import com.examsaathi.dto.response.ApiResponse;
import com.examsaathi.dto.response.UserResponse;
import com.examsaathi.entity.User;
import com.examsaathi.repository.UserRepository;
import com.examsaathi.service.AnalyticsService;
import com.examsaathi.service.UserMapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/admin")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Admin", description = "Admin-only management and analytics APIs")
public class AdminController {

    private final UserRepository userRepository;
    private final AnalyticsService analyticsService;
    private final UserMapper userMapper;

    @GetMapping("/analytics")
    @Operation(summary = "Get platform-wide analytics")
    public ResponseEntity<ApiResponse<AdminAnalyticsResponse>> getAnalytics() {
        return ResponseEntity.ok(ApiResponse.success(analyticsService.getAdminAnalytics()));
    }

    @GetMapping("/students")
    @Operation(summary = "Get all students (paginated)")
    public ResponseEntity<ApiResponse<List<UserResponse>>> getStudents(
            @RequestParam(defaultValue = "") String query,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        Page<User> studentPage = userRepository.searchStudents(query, PageRequest.of(page, size));
        List<UserResponse> students = studentPage.getContent().stream()
            .map(userMapper::toResponse).collect(Collectors.toList());
        return ResponseEntity.ok(ApiResponse.success(students));
    }

    @GetMapping("/students/{id}")
    @Operation(summary = "Get student profile")
    public ResponseEntity<ApiResponse<UserResponse>> getStudent(@PathVariable Long id) {
        User user = userRepository.findById(id)
            .orElseThrow(() -> new com.examsaathi.exception.ResourceNotFoundException("User", id));
        return ResponseEntity.ok(ApiResponse.success(userMapper.toResponse(user)));
    }

    @PatchMapping("/students/{id}/status")
    @Operation(summary = "Activate/deactivate student")
    public ResponseEntity<ApiResponse<UserResponse>> updateStudentStatus(
            @PathVariable Long id,
            @RequestParam Boolean isActive) {
        User user = userRepository.findById(id)
            .orElseThrow(() -> new com.examsaathi.exception.ResourceNotFoundException("User", id));
        user.setIsActive(isActive);
        userRepository.save(user);
        return ResponseEntity.ok(ApiResponse.success("Status updated", userMapper.toResponse(user)));
    }
}

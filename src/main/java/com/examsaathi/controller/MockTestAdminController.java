package com.examsaathi.controller;

import com.examsaathi.dto.response.ApiResponse;
import com.examsaathi.dto.response.MockTestPurgeResponse;
import com.examsaathi.service.MockTestAdminService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/admin/mock-tests")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Mock Tests", description = "Admin mock test management")
public class MockTestAdminController {

    private final MockTestAdminService mockTestAdminService;

    @DeleteMapping("/all")
    @Operation(summary = "Delete all mock test questions, configs, and student results")
    public ResponseEntity<ApiResponse<MockTestPurgeResponse>> purgeAll() {
        MockTestPurgeResponse result = mockTestAdminService.purgeAll();
        return ResponseEntity.ok(ApiResponse.success("All mock test data deleted", result));
    }
}

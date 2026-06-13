package com.examsaathi.controller;

import com.examsaathi.dto.response.ApiResponse;
import com.examsaathi.dto.response.CacheStatsResponse;
import com.examsaathi.service.CacheEvictionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/admin/cache")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Cache Admin", description = "Cache monitoring and refresh (admin only)")
public class CacheAdminController {

    private final CacheEvictionService cacheEvictionService;

    @GetMapping("/stats")
    @Operation(summary = "Cache hit/miss statistics for all caches")
    public ResponseEntity<ApiResponse<List<CacheStatsResponse>>> getStats() {
        return ResponseEntity.ok(ApiResponse.success(cacheEvictionService.getCacheStats()));
    }

    @PostMapping("/refresh")
    @Operation(summary = "Clear all in-memory caches")
    public ResponseEntity<ApiResponse<Void>> refreshAll() {
        cacheEvictionService.clearAllCaches();
        return ResponseEntity.ok(ApiResponse.success("All caches cleared", null));
    }
}

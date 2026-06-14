package com.examsaathi.controller;

import com.examsaathi.dto.response.ApiResponse;
import com.examsaathi.dto.response.SheetCacheRefreshResponse;
import com.examsaathi.dto.response.SheetValidationResponse;
import com.examsaathi.service.GoogleSheetsAdminService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/admin/google-sheets")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Google Sheets Admin", description = "Validate and manage sheet-backed mock test questions")
public class GoogleSheetsAdminController {

    private final GoogleSheetsAdminService googleSheetsAdminService;

    @GetMapping("/validate")
    @Operation(summary = "Validate Google Sheet structure and topic mappings for an exam")
    public ResponseEntity<ApiResponse<SheetValidationResponse>> validate(@RequestParam Long examId) {
        return ResponseEntity.ok(ApiResponse.success(
            googleSheetsAdminService.validateExamSheet(examId)));
    }

    @PostMapping("/refresh-cache")
    @Operation(summary = "Evict and immediately reload sheet question cache for an exam")
    public ResponseEntity<ApiResponse<SheetCacheRefreshResponse>> refreshCache(@RequestParam Long examId) {
        return ResponseEntity.ok(ApiResponse.success(
            "Sheet cache refreshed",
            googleSheetsAdminService.refreshExamSheetCache(examId)));
    }

    @PostMapping("/refresh-cache/by-sheet-id")
    @Operation(summary = "Evict and reload cache by raw sheet id")
    public ResponseEntity<ApiResponse<SheetCacheRefreshResponse>> refreshCacheBySheetId(
            @RequestParam String sheetId) {
        return ResponseEntity.ok(ApiResponse.success(
            "Sheet cache refreshed",
            googleSheetsAdminService.refreshSheetCacheBySheetId(sheetId)));
    }
}

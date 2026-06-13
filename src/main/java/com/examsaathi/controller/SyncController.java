package com.examsaathi.controller;

import com.examsaathi.dto.request.SyncPushRequest;
import com.examsaathi.dto.response.ApiResponse;
import com.examsaathi.dto.response.SyncBundleResponse;
import com.examsaathi.dto.response.SyncCatalogResponse;
import com.examsaathi.entity.User;
import com.examsaathi.repository.UserRepository;
import com.examsaathi.service.SyncService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/sync")
@RequiredArgsConstructor
@Tag(name = "Sync", description = "Offline-first delta sync APIs")
public class SyncController {

    private final SyncService syncService;
    private final UserRepository userRepository;

    @GetMapping("/catalog")
    @Operation(summary = "Delta sync for master catalog (exams, subjects, chapters, topics)")
    public ResponseEntity<ApiResponse<SyncCatalogResponse>> syncCatalog(
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime since,
            @RequestParam(required = false) List<Long> examIds) {
        return ResponseEntity.ok(ApiResponse.success(syncService.getCatalogSync(since, examIds)));
    }

    @GetMapping("/bundle")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "Bundled sync for dashboard, my exams, and subject progress")
    public ResponseEntity<ApiResponse<SyncBundleResponse>> syncBundle(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime since) {
        User user = userRepository.findByEmail(userDetails.getUsername()).orElseThrow();
        return ResponseEntity.ok(ApiResponse.success(syncService.getBundleSync(user.getId(), since)));
    }

    @PostMapping("/push")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "Push offline-queued user changes")
    public ResponseEntity<ApiResponse<Void>> pushChanges(
            @AuthenticationPrincipal UserDetails userDetails,
            @Valid @RequestBody SyncPushRequest request) {
        User user = userRepository.findByEmail(userDetails.getUsername()).orElseThrow();
        syncService.pushOfflineChanges(user.getId(), request);
        return ResponseEntity.ok(ApiResponse.success("Sync applied", null));
    }
}

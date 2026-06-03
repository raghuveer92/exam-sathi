package com.examsaathi.controller;

import com.examsaathi.dto.request.TopicTestConfigRequest;
import com.examsaathi.dto.response.ApiResponse;
import com.examsaathi.dto.response.TopicTestConfigResponse;
import com.examsaathi.service.TopicTestConfigService;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/admin/topic-tests")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Topic Tests", description = "Admin topic test configuration")
public class TopicTestAdminController {

    private final TopicTestConfigService topicTestConfigService;

    @GetMapping
    public ResponseEntity<ApiResponse<List<TopicTestConfigResponse>>> list() {
        return ResponseEntity.ok(ApiResponse.success(topicTestConfigService.listAll()));
    }

    @GetMapping("/topic/{topicId}")
    public ResponseEntity<ApiResponse<TopicTestConfigResponse>> getByTopic(@PathVariable Long topicId) {
        return ResponseEntity.ok(ApiResponse.success(topicTestConfigService.getByTopicId(topicId)));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<TopicTestConfigResponse>> upsert(
            @Valid @RequestBody TopicTestConfigRequest request) {
        return ResponseEntity.ok(ApiResponse.success(
            "Topic test config saved", topicTestConfigService.upsert(request)));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable Long id) {
        topicTestConfigService.delete(id);
        return ResponseEntity.ok(ApiResponse.success("Topic test config deleted", null));
    }
}

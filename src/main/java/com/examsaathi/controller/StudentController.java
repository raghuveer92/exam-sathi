package com.examsaathi.controller;

import com.examsaathi.dto.request.EnrollExamRequest;
import com.examsaathi.dto.request.ExamGoalRequest;
import com.examsaathi.dto.request.SubjectGroupSelectionRequest;
import com.examsaathi.dto.request.UpdateStudyHoursRequest;
import com.examsaathi.dto.request.UserExamCreateRequest;
import com.examsaathi.dto.request.UserExamDateUpdateRequest;
import com.examsaathi.dto.response.ApiResponse;
import com.examsaathi.dto.response.DashboardResponse;
import com.examsaathi.dto.response.ExamSubjectGroupResponse;
import com.examsaathi.dto.response.UserResponse;
import com.examsaathi.dto.response.UserExamResponse;
import com.examsaathi.entity.Exam;
import com.examsaathi.entity.User;
import com.examsaathi.entity.UserExam;
import com.examsaathi.exception.BadRequestException;
import com.examsaathi.repository.ExamRepository;
import com.examsaathi.repository.StudyProgressRepository;
import com.examsaathi.repository.TopicRepository;
import com.examsaathi.repository.UserExamRepository;
import com.examsaathi.repository.UserRepository;
import com.examsaathi.service.DashboardService;
import com.examsaathi.service.ExamSubjectGroupService;
import com.examsaathi.service.UserMapper;
import jakarta.validation.Valid;
import jakarta.transaction.Transactional;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/student")
@RequiredArgsConstructor
@Tag(name = "Student", description = "Student profile and dashboard APIs")
@SecurityRequirement(name = "bearerAuth")
public class StudentController {

    private final UserRepository userRepository;
    private final UserExamRepository userExamRepository;
    private final ExamRepository examRepository;
    private final DashboardService dashboardService;
    private final UserMapper userMapper;
    private final TopicRepository topicRepository;
    private final StudyProgressRepository studyProgressRepository;
    private final ExamSubjectGroupService examSubjectGroupService;

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
    @Operation(summary = "Select exam to prepare for (backward-compatible)")
    public ResponseEntity<ApiResponse<UserResponse>> selectExam(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable Long examId) {
        User user = userRepository.findByEmailWithExam(userDetails.getUsername()).orElseThrow();
        UserExam userExam = upsertUserExam(user, examId, null, null, null);
        if (examSubjectGroupService.hasRequiredOptionalGroups(examId) && !examSubjectGroupService.hasValidSelections(userExam)) {
            throw new BadRequestException("Subject selections are required for this exam");
        }
        setActiveExam(user, userExam);
        userRepository.save(user);
        return ResponseEntity.ok(ApiResponse.success("Exam selected", userMapper.toResponse(user)));
    }

    @GetMapping("/my-exams")
    @Transactional
    @Operation(summary = "Get all exams selected by current student")
    public ResponseEntity<ApiResponse<List<UserExamResponse>>> getMyExams(
            @AuthenticationPrincipal UserDetails userDetails) {
        User user = userRepository.findByEmailWithExam(userDetails.getUsername()).orElseThrow();
        List<UserExamResponse> exams = userExamRepository.findByUserIdOrderByExamDateAscCreatedAtAsc(user.getId())
            .stream()
            .map(ue -> toUserExamCard(user.getId(), ue))
            .collect(Collectors.toList());
        return ResponseEntity.ok(ApiResponse.success(exams));
    }

    @PostMapping("/my-exams")
    @Transactional
    @Operation(summary = "Add an exam for current student and set it active")
    public ResponseEntity<ApiResponse<UserResponse>> addMyExam(
            @AuthenticationPrincipal UserDetails userDetails,
            @Valid @RequestBody UserExamCreateRequest request) {
        User user = userRepository.findByEmailWithExam(userDetails.getUsername()).orElseThrow();
        UserExam userExam = upsertUserExam(user, request.getExamId(), request.getExamDate(),
            request.getDailyTargetHours(), request.getExperienceLevel());
        examSubjectGroupService.saveSelections(userExam, request.getSubjectSelections());
        setActiveExam(user, userExam);
        userRepository.save(user);
        return ResponseEntity.ok(ApiResponse.success("Exam added", userMapper.toResponse(user)));
    }

    @PostMapping("/my-exams/enroll")
    @Transactional
    @Operation(summary = "Enroll in exam with goal settings (wizard confirm)")
    public ResponseEntity<ApiResponse<UserResponse>> enrollExam(
            @AuthenticationPrincipal UserDetails userDetails,
            @Valid @RequestBody EnrollExamRequest request) {
        User user = userRepository.findByEmailWithExam(userDetails.getUsername()).orElseThrow();
        UserExam userExam = upsertUserExam(user, request.getExamId(), request.getExamDate(),
            request.getDailyTargetHours(), request.getExperienceLevel());

        LocalDate syllabusDate = request.getSyllabusTargetDate() != null
            ? request.getSyllabusTargetDate()
            : request.getExamDate().minusDays(30);
        userExam.setSyllabusTargetDate(syllabusDate);

        if (request.getDailyTargetHours() == null && userExam.getExam() != null) {
            applyAutoStudyHours(userExam, request.getExamDate());
        } else if (request.getDailyTargetHours() != null) {
            double daily = Math.max(0.5, Math.min(16.0, request.getDailyTargetHours()));
            daily = Math.round(daily * 2.0) / 2.0;
            userExam.setDailyTargetHours(daily);
            userExam.setWeeklyTargetHours(Math.round(daily * 7 * 10.0) / 10.0);
        }

        userExamRepository.save(userExam);
        examSubjectGroupService.saveSelections(userExam, request.getSubjectSelections());
        setActiveExam(user, userExam);
        userRepository.save(user);
        return ResponseEntity.ok(ApiResponse.success("Study plan created", userMapper.toResponse(user)));
    }

    @GetMapping("/exams/{examId}/subject-groups")
    @Transactional
    @Operation(summary = "Get subject groups for an exam, including current user selections if available")
    public ResponseEntity<ApiResponse<List<ExamSubjectGroupResponse>>> getExamSubjectGroups(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable Long examId) {
        User user = userRepository.findByEmailWithExam(userDetails.getUsername()).orElseThrow();
        return ResponseEntity.ok(ApiResponse.success(
            userExamRepository.findByUserIdAndExamId(user.getId(), examId)
                .map(userExam -> examSubjectGroupService.getGroupsByUserExam(userExam.getId()))
                .orElseGet(() -> examSubjectGroupService.getGroupsByExam(examId))
        ));
    }

    @PutMapping("/my-exams/{userExamId}/subject-selections")
    @Transactional
    @Operation(summary = "Update optional subject selections for a user exam")
    public ResponseEntity<ApiResponse<UserResponse>> updateSubjectSelections(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable Long userExamId,
            @RequestBody List<SubjectGroupSelectionRequest> request) {
        User user = userRepository.findByEmailWithExam(userDetails.getUsername()).orElseThrow();
        UserExam userExam = userExamRepository.findById(userExamId)
            .filter(ue -> ue.getUser().getId().equals(user.getId()))
            .orElseThrow(() -> new BadRequestException("Exam mapping not found"));
        examSubjectGroupService.saveSelections(userExam, request);
        return ResponseEntity.ok(ApiResponse.success("Subject selections updated", userMapper.toResponse(user)));
    }

    @PatchMapping("/my-exams/{userExamId}/date")
    @Transactional
    @Operation(summary = "Edit target exam date")
    public ResponseEntity<ApiResponse<UserResponse>> updateExamDate(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable Long userExamId,
            @Valid @RequestBody UserExamDateUpdateRequest request) {
        User user = userRepository.findByEmailWithExam(userDetails.getUsername()).orElseThrow();
        UserExam userExam = userExamRepository.findById(userExamId)
            .filter(ue -> ue.getUser().getId().equals(user.getId()))
            .orElseThrow(() -> new BadRequestException("Exam mapping not found"));

        userExam.setExamDate(request.getExamDate());
        userExamRepository.save(userExam);

        if (Boolean.TRUE.equals(userExam.getIsActive())) {
            user.setExamDate(request.getExamDate());
            userRepository.save(user);
        }

        return ResponseEntity.ok(ApiResponse.success("Exam date updated", userMapper.toResponse(user)));
    }

    @PatchMapping("/my-exams/{userExamId}/active")
    @Transactional
    @Operation(summary = "Set active exam")
    public ResponseEntity<ApiResponse<UserResponse>> setActiveExamEndpoint(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable Long userExamId) {
        User user = userRepository.findByEmailWithExam(userDetails.getUsername()).orElseThrow();
        UserExam userExam = userExamRepository.findById(userExamId)
            .filter(ue -> ue.getUser().getId().equals(user.getId()))
            .orElseThrow(() -> new BadRequestException("Exam mapping not found"));

        if (examSubjectGroupService.hasRequiredOptionalGroups(userExam.getExam().getId())
            && !examSubjectGroupService.hasValidSelections(userExam)) {
            throw new BadRequestException("Subject selections are required for this exam");
        }

        setActiveExam(user, userExam);
        userRepository.save(user);
        return ResponseEntity.ok(ApiResponse.success("Active exam updated", userMapper.toResponse(user)));
    }

    @DeleteMapping("/my-exams/{userExamId}")
    @Transactional
    @Operation(summary = "Delete an exam from current student")
    public ResponseEntity<ApiResponse<UserResponse>> deleteMyExam(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable Long userExamId) {
        User user = userRepository.findByEmailWithExam(userDetails.getUsername()).orElseThrow();
        List<UserExam> all = userExamRepository.findByUserIdOrderByCreatedAtAsc(user.getId());
        if (all.size() <= 1) {
            throw new BadRequestException("At least one exam must exist");
        }

        UserExam toDelete = all.stream()
            .filter(ue -> ue.getId().equals(userExamId))
            .findFirst()
            .orElseThrow(() -> new BadRequestException("Exam mapping not found"));

        boolean deletingActive = Boolean.TRUE.equals(toDelete.getIsActive());
        // Keep JPA managed state in sync so deleted exams do not reappear in the same transaction.
        user.getUserExams().removeIf(ue -> ue.getId().equals(toDelete.getId()));
        userExamRepository.delete(toDelete);
        userExamRepository.flush();

        if (deletingActive) {
            List<UserExam> remaining = userExamRepository.findByUserIdOrderByExamDateAscCreatedAtAsc(user.getId());
            UserExam next = remaining.stream()
                .sorted(Comparator
                    .comparing((UserExam ue) -> ue.getExamDate() == null ? LocalDate.MAX : ue.getExamDate())
                    .thenComparing(UserExam::getCreatedAt, Comparator.nullsLast(Comparator.naturalOrder())))
                .findFirst()
                .orElseThrow(() -> new BadRequestException("No exam found after delete"));
            setActiveExam(user, next);
        }
        userRepository.saveAndFlush(user);

        User refreshedUser = userRepository.findByEmailWithExam(userDetails.getUsername()).orElseThrow();
        return ResponseEntity.ok(ApiResponse.success("Exam deleted", userMapper.toResponse(refreshedUser)));
    }

    @PostMapping("/exam-goal")
    @Transactional
    @Operation(summary = "Set exam date and study targets")
    public ResponseEntity<ApiResponse<UserResponse>> setExamGoal(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestBody ExamGoalRequest request) {
        User user = userRepository.findByEmailWithExam(userDetails.getUsername()).orElseThrow();
        UserExam userExam;
        if (request.getUserExamId() != null) {
            userExam = userExamRepository.findById(request.getUserExamId())
                .filter(ue -> ue.getUser().getId().equals(user.getId()))
                .orElseThrow(() -> new BadRequestException("Exam mapping not found"));
        } else {
            userExam = userExamRepository.findByUserIdAndIsActiveTrue(user.getId())
                .orElseThrow(() -> new BadRequestException("No active exam selected"));
        }

        userExam.setExamDate(request.getExamDate());

        LocalDate syllabusDate = request.getSyllabusTargetDate() != null
            ? request.getSyllabusTargetDate()
            : request.getExamDate().minusDays(30);
        userExam.setSyllabusTargetDate(syllabusDate);

        if (userExam.getExam() != null) {
            List<Long> visibleSubjectIds = examSubjectGroupService.getVisibleSubjectIds(userExam);
            Double totalHours = visibleSubjectIds.isEmpty() ? 0.0 : topicRepository.sumEstimatedHoursBySubjectIds(visibleSubjectIds);
            long daysRemaining = ChronoUnit.DAYS.between(LocalDate.now(), request.getExamDate());
            if (totalHours != null && totalHours > 0 && daysRemaining > 0) {
                double daily = Math.round((totalHours / daysRemaining) * 10.0) / 10.0;
                userExam.setDailyTargetHours(daily);
                userExam.setWeeklyTargetHours(Math.round(daily * 7 * 10.0) / 10.0);
            }
        }

        userExamRepository.save(userExam);
        if (Boolean.TRUE.equals(userExam.getIsActive())) {
            syncUserFromUserExam(user, userExam);
        }

        userRepository.save(user);
        return ResponseEntity.ok(ApiResponse.success("Exam goal set", userMapper.toResponse(user)));
    }

    @PatchMapping("/study-hours")
    @Transactional
    @Operation(summary = "Update daily study target hours")
    public ResponseEntity<ApiResponse<UserResponse>> updateStudyHours(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestBody UpdateStudyHoursRequest request) {
        User user = userRepository.findByEmailWithExam(userDetails.getUsername()).orElseThrow();
        if (request.getDailyTargetHours() != null) {
            double daily = Math.max(0.5, Math.min(16.0, request.getDailyTargetHours()));
            daily = Math.round(daily * 2.0) / 2.0; // snap to 0.5 increments
            user.setDailyTargetHours(daily);
            user.setWeeklyTargetHours(Math.round(daily * 7 * 10.0) / 10.0);
            final double finalDaily = daily;

            userExamRepository.findByUserIdAndIsActiveTrue(user.getId()).ifPresent(active -> {
                active.setDailyTargetHours(finalDaily);
                active.setWeeklyTargetHours(Math.round(finalDaily * 7 * 10.0) / 10.0);
                userExamRepository.save(active);
            });
        }
        userRepository.save(user);
        return ResponseEntity.ok(ApiResponse.success("Study hours updated", userMapper.toResponse(user)));
    }

    private UserExam upsertUserExam(User user, Long examId, LocalDate examDate,
                                    Double dailyTargetHours, String experienceLevel) {
        Exam exam = examRepository.findById(examId)
            .orElseThrow(() -> new BadRequestException("Invalid exam id: " + examId));

        UserExam userExam = userExamRepository.findByUserIdAndExamId(user.getId(), examId)
            .orElseGet(() -> UserExam.builder()
                .user(user)
                .exam(exam)
                .isActive(false)
                .build());

        if (examDate != null) {
            userExam.setExamDate(examDate);
        }
        if (experienceLevel != null && !experienceLevel.isBlank()) {
            userExam.setExperienceLevel(experienceLevel.trim().toUpperCase());
        }
        if (dailyTargetHours != null) {
            double daily = Math.max(0.5, Math.min(16.0, dailyTargetHours));
            daily = Math.round(daily * 2.0) / 2.0;
            userExam.setDailyTargetHours(daily);
            userExam.setWeeklyTargetHours(Math.round(daily * 7 * 10.0) / 10.0);
        }
        return userExamRepository.save(userExam);
    }

    private void applyAutoStudyHours(UserExam userExam, LocalDate examDate) {
        List<Long> visibleSubjectIds = examSubjectGroupService.getVisibleSubjectIds(userExam);
        Double totalHours = visibleSubjectIds.isEmpty() ? 0.0
            : topicRepository.sumEstimatedHoursBySubjectIds(visibleSubjectIds);
        long daysRemaining = ChronoUnit.DAYS.between(LocalDate.now(), examDate);
        if (totalHours != null && totalHours > 0 && daysRemaining > 0) {
            double daily = Math.round((totalHours / daysRemaining) * 10.0) / 10.0;
            userExam.setDailyTargetHours(daily);
            userExam.setWeeklyTargetHours(Math.round(daily * 7 * 10.0) / 10.0);
        }
    }

    private void setActiveExam(User user, UserExam activeUserExam) {
        List<UserExam> all = userExamRepository.findByUserIdOrderByCreatedAtAsc(user.getId());
        for (UserExam ue : all) {
            ue.setIsActive(ue.getId().equals(activeUserExam.getId()));
        }
        userExamRepository.saveAll(all);
        syncUserFromUserExam(user, activeUserExam);
    }

    private void syncUserFromUserExam(User user, UserExam userExam) {
        user.setSelectedExam(userExam.getExam());
        user.setExamDate(userExam.getExamDate());
        user.setSyllabusTargetDate(userExam.getSyllabusTargetDate());
        user.setDailyTargetHours(userExam.getDailyTargetHours());
        user.setWeeklyTargetHours(userExam.getWeeklyTargetHours());
    }

    private UserExamResponse toUserExamCard(Long userId, UserExam ue) {
        Integer daysLeft = null;
        if (ue.getExamDate() != null) {
            daysLeft = (int) ChronoUnit.DAYS.between(LocalDate.now(), ue.getExamDate());
        }
        Long examId = ue.getExam().getId();
        List<Long> visibleSubjectIds = examSubjectGroupService.getVisibleSubjectIds(ue);
        int totalSubjects = visibleSubjectIds.size();
        int totalTopics = visibleSubjectIds.isEmpty() ? 0 : topicRepository.findBySubjectIdIn(visibleSubjectIds).size();
        int completedTopics = visibleSubjectIds.isEmpty() ? 0 : studyProgressRepository.countCompletedByUserExamIdAndSubjectIds(ue.getId(), visibleSubjectIds);
        Double progress = totalTopics > 0 ? (completedTopics * 100.0 / totalTopics) : 0.0;
        return UserExamResponse.builder()
            .id(ue.getId())
            .examId(examId)
            .examName(ue.getExam().getName())
            .examDate(ue.getExamDate())
            .dailyTargetHours(ue.getDailyTargetHours())
            .experienceLevel(ue.getExperienceLevel())
            .daysLeft(daysLeft)
            .totalSubjects(totalSubjects)
            .progressPercent(progress)
            .isActive(ue.getIsActive())
            .subjectGroups(examSubjectGroupService.getGroupsByUserExam(ue.getId()))
            .createdAt(ue.getCreatedAt())
            .build();
    }
}

package com.examsaathi.service;

import com.examsaathi.dto.response.DashboardResponse;
import com.examsaathi.dto.response.SubjectProgressResponse;
import com.examsaathi.dto.response.UserExamResponse;
import com.examsaathi.entity.StudyProgress;
import com.examsaathi.entity.User;
import com.examsaathi.entity.UserExam;
import com.examsaathi.repository.StudyProgressRepository;
import com.examsaathi.repository.UserExamRepository;
import com.examsaathi.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Read-only loaders for {@link SyncService#getBundleSync}. Each method runs in its own
 * transaction so parallel CompletableFuture tasks can access the DB safely.
 */
@Service
@RequiredArgsConstructor
public class SyncBundleLoader {

    private final UserRepository userRepository;
    private final UserExamRepository userExamRepository;
    private final DashboardService dashboardService;
    private final StudyProgressService studyProgressService;
    private final StudyProgressRepository studyProgressRepository;
    private final UserMapper mapper;

    public record UserSyncContext(User user, List<UserExam> userExams, List<UserExamResponse> myExams) {}

    @Transactional(readOnly = true)
    public UserSyncContext loadUserContext(Long userId) {
        User user = userRepository.findById(userId).orElseThrow();
        List<UserExam> userExams = userExamRepository.findByUserIdWithExamOrderByCreatedAtAsc(userId);
        List<UserExamResponse> myExams = userExams.stream().map(mapper::toUserExamResponse).toList();
        return new UserSyncContext(user, userExams, myExams);
    }

    @Transactional(readOnly = true)
    public DashboardResponse loadDashboard(Long userId) {
        return dashboardService.getDashboard(userId);
    }

    @Transactional(readOnly = true)
    public Map<Long, List<SubjectProgressResponse>> loadSubjectProgressByExam(Long userId, List<UserExam> userExams) {
        return studyProgressService.getSubjectProgressByExams(userId, userExams);
    }

    @Transactional(readOnly = true)
    public List<StudyProgress> loadChangedProgressEntities(Long userId, LocalDateTime since, boolean fullSync) {
        if (fullSync) {
            return studyProgressRepository.findByUserIdWithDetails(userId);
        }
        return studyProgressRepository.findByUserIdAndUpdatedAtAfter(userId, since);
    }

    @Transactional(readOnly = true)
    public List<SubjectProgressResponse> loadSubjectProgress(Long userId, Long examId) {
        return studyProgressService.getSubjectProgress(userId, examId);
    }
}

package com.examsaathi.service;

import com.examsaathi.entity.Role;
import com.examsaathi.entity.User;
import com.examsaathi.exception.BadRequestException;
import com.examsaathi.repository.DailyStudyLogRepository;
import com.examsaathi.repository.StudyProgressRepository;
import com.examsaathi.repository.TestAttemptRepository;
import com.examsaathi.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Account lifecycle operations (self-service deletion).
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AccountService {

    private final UserRepository userRepository;
    private final TestAttemptRepository testAttemptRepository;
    private final DailyStudyLogRepository dailyStudyLogRepository;
    private final StudyProgressRepository studyProgressRepository;
    private final AuthenticationManager authenticationManager;

    /** Permanently delete the authenticated student's account and related data. */
    @Transactional
    public void deleteStudentAccount(String email, String password) {
        User user = userRepository.findByEmail(email)
            .orElseThrow(() -> new BadRequestException("User not found"));

        boolean isAdmin = user.getRoles().stream()
            .anyMatch(role -> role.getName() == Role.RoleName.ROLE_ADMIN);
        if (isAdmin) {
            throw new BadRequestException("Admin accounts cannot be deleted from the student app");
        }

        authenticationManager.authenticate(
            new UsernamePasswordAuthenticationToken(email, password));

        Long userId = user.getId();
        testAttemptRepository.deleteByUserId(userId);
        dailyStudyLogRepository.deleteByUserId(userId);
        studyProgressRepository.deleteByUserId(userId);

        user.getRoles().clear();
        user.getUserExams().clear();
        userRepository.delete(user);

        log.info("Student account deleted: userId={}", userId);
    }
}

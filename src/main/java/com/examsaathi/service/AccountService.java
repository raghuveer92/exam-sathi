package com.examsaathi.service;

import com.examsaathi.entity.Role;
import com.examsaathi.entity.User;
import com.examsaathi.exception.BadRequestException;
import com.examsaathi.repository.DailyStudyLogRepository;
import com.examsaathi.repository.StudyProgressRepository;
import com.examsaathi.repository.TestAttemptRepository;
import com.examsaathi.repository.UserRepository;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken;
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
    private final GoogleTokenVerifier googleTokenVerifier;

    /** Permanently delete the authenticated student's account and related data. */
    @Transactional
    public void deleteStudentAccount(String email, String password, String idToken) {
        User user = userRepository.findByEmail(email)
            .orElseThrow(() -> new BadRequestException("User not found"));

        boolean isAdmin = user.getRoles().stream()
            .anyMatch(role -> role.getName() == Role.RoleName.ROLE_ADMIN);
        if (isAdmin) {
            throw new BadRequestException("Admin accounts cannot be deleted from the student app");
        }

        verifyDeletionConfirmation(user, password, idToken);

        Long userId = user.getId();
        testAttemptRepository.deleteByUserId(userId);
        dailyStudyLogRepository.deleteByUserId(userId);
        studyProgressRepository.deleteByUserId(userId);

        user.getRoles().clear();
        user.getUserExams().clear();
        userRepository.delete(user);

        log.info("Student account deleted: userId={}", userId);
    }

    private void verifyDeletionConfirmation(User user, String password, String idToken) {
        boolean hasIdToken = idToken != null && !idToken.isBlank();
        boolean hasPassword = password != null && !password.isBlank();

        if (hasIdToken) {
            GoogleIdToken.Payload payload = googleTokenVerifier.verify(idToken.trim());
            String tokenEmail = payload.getEmail();
            if (tokenEmail == null || !tokenEmail.equalsIgnoreCase(user.getEmail())) {
                throw new BadRequestException("Google account does not match this user");
            }
            String googleSub = payload.getSubject();
            if (user.getGoogleId() != null && !user.getGoogleId().equals(googleSub)) {
                throw new BadRequestException("Google account does not match this user");
            }
            return;
        }

        if (user.getPassword() == null) {
            throw new BadRequestException(
                "This account uses Google Sign-In. Confirm deletion by signing in with Google again.");
        }

        if (!hasPassword) {
            throw new BadRequestException("Password is required");
        }

        authenticationManager.authenticate(
            new UsernamePasswordAuthenticationToken(user.getEmail(), password));
    }
}

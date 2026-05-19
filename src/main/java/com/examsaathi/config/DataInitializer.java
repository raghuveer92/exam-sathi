package com.examsaathi.config;

import com.examsaathi.entity.*;
import com.examsaathi.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Set;

/**
 * DataInitializer — seeds roles, admin user, and sample exams on first boot.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class DataInitializer implements CommandLineRunner {

    private final RoleRepository roleRepository;
    private final UserRepository userRepository;
    private final ExamRepository examRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    @Transactional
    public void run(String... args) {
        seedRoles();
        seedAdminUser();
        seedExams();
    }

    private void seedRoles() {
        for (Role.RoleName roleName : Role.RoleName.values()) {
            if (roleRepository.findByName(roleName).isEmpty()) {
                roleRepository.save(Role.builder().name(roleName).build());
                log.info("Seeded role: {}", roleName);
            }
        }
    }

    private void seedAdminUser() {
        String adminEmail = "admin@examsaathi.com";
        if (userRepository.existsByEmail(adminEmail)) return;

        Role adminRole = roleRepository.findByName(Role.RoleName.ROLE_ADMIN).orElseThrow();
        User admin = User.builder()
            .fullName("ExamSaathi Admin")
            .email(adminEmail)
            .password(passwordEncoder.encode("Admin@123"))
            .isActive(true)
            .isEmailVerified(true)
            .roles(Set.of(adminRole))
            .build();
        userRepository.save(admin);
        log.info("Admin user seeded: {} / Admin@123", adminEmail);
    }

    private void seedExams() {
        if (examRepository.count() > 0) return;

        String[][] exams = {
            {"CBSE Class 10", "CBSE10", "#6C63FF"},
            {"CBSE Class 12", "CBSE12", "#FF6584"},
            {"NEET", "NEET", "#43D854"},
            {"JEE Main", "JEE_MAIN", "#FF9F43"},
            {"UPSC Civil Services", "UPSC", "#54A0FF"},
            {"SSC CGL", "SSC_CGL", "#5F27CD"}
        };

        for (String[] exam : exams) {
            Exam e = Exam.builder()
                .name(exam[0])
                .code(exam[1])
                .colorCode(exam[2])
                .isActive(true)
                .build();
            examRepository.save(e);
        }
        log.info("Seeded {} exams", exams.length);
    }
}

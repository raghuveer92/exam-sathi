package com.examsaathi.integration;

import com.examsaathi.dto.request.ForgotPasswordRequest;
import com.examsaathi.dto.request.LoginRequest;
import com.examsaathi.dto.request.RegisterRequest;
import com.examsaathi.dto.request.ResetPasswordRequest;
import com.examsaathi.dto.request.VerifyEmailOtpRequest;
import com.examsaathi.entity.AuthProvider;
import com.examsaathi.entity.OtpPurpose;
import com.examsaathi.entity.User;
import com.examsaathi.repository.UserRepository;
import com.examsaathi.service.EmailService;
import com.examsaathi.service.OtpService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AuthEmailFlowIntegrationTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @Autowired private UserRepository userRepository;
    @Autowired private PasswordEncoder passwordEncoder;
    @Autowired private OtpService otpService;

    @MockBean private EmailService emailService;

    @BeforeEach
    void setUp() {
        userRepository.deleteAll();
        doNothing().when(emailService).sendVerificationOtpEmail(any(User.class), anyString());
        doNothing().when(emailService).sendPasswordResetOtpEmail(any(User.class), anyString());
    }

    @Test
    void registrationIssuesOtp() throws Exception {
        RegisterRequest request = new RegisterRequest();
        request.setFullName("Integration Student");
        request.setEmail("integration@example.com");
        request.setPassword("secret12");

        mockMvc.perform(post("/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.data.email").value("integration@example.com"));

        User user = userRepository.findByEmail("integration@example.com").orElseThrow();
        assertThat(user.getEmailOtp()).matches("\\d{6}");
        assertThat(user.getEmailOtpExpiry()).isAfter(LocalDateTime.now());
        assertThat(user.getOtpPurpose()).isEqualTo(OtpPurpose.VERIFY_EMAIL);
        assertThat(user.getIsEmailVerified()).isFalse();

        ArgumentCaptor<String> otpCaptor = ArgumentCaptor.forClass(String.class);
        verify(emailService).sendVerificationOtpEmail(any(User.class), otpCaptor.capture());
        assertThat(otpCaptor.getValue()).isEqualTo(user.getEmailOtp());
    }

    @Test
    void fullVerificationAndLoginFlow() throws Exception {
        String otp = registerUser("verified-flow@example.com");

        VerifyEmailOtpRequest verifyRequest = new VerifyEmailOtpRequest();
        verifyRequest.setEmail("verified-flow@example.com");
        verifyRequest.setOtp(otp);

        mockMvc.perform(post("/auth/verify-email-otp")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(verifyRequest)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.message").value("Email verified successfully"));

        User user = userRepository.findByEmail("verified-flow@example.com").orElseThrow();
        assertThat(user.getIsEmailVerified()).isTrue();
        assertThat(user.getEmailOtp()).isNull();

        LoginRequest loginRequest = new LoginRequest();
        loginRequest.setEmail("verified-flow@example.com");
        loginRequest.setPassword("secret12");

        mockMvc.perform(post("/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(loginRequest)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.accessToken").exists());
    }

    @Test
    void loginBlockedForUnverifiedUser() throws Exception {
        registerUser("blocked@example.com");

        LoginRequest loginRequest = new LoginRequest();
        loginRequest.setEmail("blocked@example.com");
        loginRequest.setPassword("secret12");

        mockMvc.perform(post("/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(loginRequest)))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.message").value("Please verify your email using the OTP sent to your email."));
    }

    @Test
    void forgotPasswordAndResetFlow() throws Exception {
        User user = seedVerifiedUser("reset-flow@example.com");

        ForgotPasswordRequest forgotRequest = new ForgotPasswordRequest();
        forgotRequest.setEmail("reset-flow@example.com");

        mockMvc.perform(post("/auth/forgot-password")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(forgotRequest)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.message").value("If an account exists, an OTP has been sent."));

        User updated = userRepository.findByEmail("reset-flow@example.com").orElseThrow();
        assertThat(updated.getEmailOtp()).matches("\\d{6}");
        assertThat(updated.getOtpPurpose()).isEqualTo(OtpPurpose.FORGOT_PASSWORD);

        ResetPasswordRequest resetRequest = new ResetPasswordRequest();
        resetRequest.setEmail("reset-flow@example.com");
        resetRequest.setOtp(updated.getEmailOtp());
        resetRequest.setNewPassword("newsecret1");

        mockMvc.perform(post("/auth/reset-password")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(resetRequest)))
            .andExpect(status().isOk());

        User resetUser = userRepository.findByEmail("reset-flow@example.com").orElseThrow();
        assertThat(resetUser.getEmailOtp()).isNull();
        assertThat(passwordEncoder.matches("newsecret1", resetUser.getPassword())).isTrue();
    }

    @Test
    void resetPasswordFailsForExpiredOtp() throws Exception {
        User user = seedVerifiedUser("expired-reset@example.com");
        user.setEmailOtp("483921");
        user.setEmailOtpExpiry(LocalDateTime.now().minusMinutes(1));
        user.setOtpPurpose(OtpPurpose.FORGOT_PASSWORD);
        userRepository.save(user);

        ResetPasswordRequest resetRequest = new ResetPasswordRequest();
        resetRequest.setEmail("expired-reset@example.com");
        resetRequest.setOtp("483921");
        resetRequest.setNewPassword("newsecret1");

        mockMvc.perform(post("/auth/reset-password")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(resetRequest)))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.message").value("OTP has expired"));
    }

    @Test
    void verifyEmailOtpFailsForInvalidOtp() throws Exception {
        registerUser("invalid-otp@example.com");

        VerifyEmailOtpRequest verifyRequest = new VerifyEmailOtpRequest();
        verifyRequest.setEmail("invalid-otp@example.com");
        verifyRequest.setOtp("000000");

        mockMvc.perform(post("/auth/verify-email-otp")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(verifyRequest)))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.message").value("Invalid OTP"));
    }

    private String registerUser(String email) throws Exception {
        RegisterRequest request = new RegisterRequest();
        request.setFullName("Test User");
        request.setEmail(email);
        request.setPassword("secret12");

        mockMvc.perform(post("/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isCreated());

        return userRepository.findByEmail(email).orElseThrow().getEmailOtp();
    }

    private User seedVerifiedUser(String email) {
        User user = User.builder()
            .fullName("Verified User")
            .email(email)
            .password(passwordEncoder.encode("secret12"))
            .authProvider(AuthProvider.EMAIL)
            .isEmailVerified(true)
            .isActive(true)
            .build();
        return userRepository.save(user);
    }
}

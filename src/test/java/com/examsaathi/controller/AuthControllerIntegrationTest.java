package com.examsaathi.controller;

import com.examsaathi.dto.request.ForgotPasswordRequest;
import com.examsaathi.dto.request.LoginRequest;
import com.examsaathi.dto.request.RegisterRequest;
import com.examsaathi.dto.request.ResetPasswordRequest;
import com.examsaathi.dto.request.VerifyEmailOtpRequest;
import com.examsaathi.dto.response.RegisterResponse;
import com.examsaathi.exception.BadRequestException;
import com.examsaathi.exception.GlobalExceptionHandler;
import com.examsaathi.service.AuthService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class AuthControllerIntegrationTest {

    @Mock private AuthService authService;
    @InjectMocks private AuthController authController;

    private MockMvc mockMvc;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(authController)
            .setControllerAdvice(new GlobalExceptionHandler())
            .build();
        objectMapper = new ObjectMapper();
        objectMapper.findAndRegisterModules();
    }

    @Test
    void registerReturnsCreated() throws Exception {
        RegisterRequest request = new RegisterRequest();
        request.setFullName("Student");
        request.setEmail("student@example.com");
        request.setPassword("secret12");

        when(authService.register(any(RegisterRequest.class))).thenReturn(
            RegisterResponse.builder().email("student@example.com").fullName("Student").build());

        mockMvc.perform(post("/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.data.email").value("student@example.com"));
    }

    @Test
    void verifyEmailOtpReturnsSuccess() throws Exception {
        VerifyEmailOtpRequest request = new VerifyEmailOtpRequest();
        request.setEmail("student@example.com");
        request.setOtp("483921");

        doNothing().when(authService).verifyEmailOtp(any(VerifyEmailOtpRequest.class));

        mockMvc.perform(post("/auth/verify-email-otp")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.message").value("Email verified successfully"));
    }

    @Test
    void verifyEmailOtpReturnsBadRequestForExpiredOtp() throws Exception {
        VerifyEmailOtpRequest request = new VerifyEmailOtpRequest();
        request.setEmail("student@example.com");
        request.setOtp("483921");

        doThrow(new BadRequestException("OTP has expired"))
            .when(authService).verifyEmailOtp(any(VerifyEmailOtpRequest.class));

        mockMvc.perform(post("/auth/verify-email-otp")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.message").value("OTP has expired"));
    }

    @Test
    void forgotPasswordReturnsGenericSuccess() throws Exception {
        ForgotPasswordRequest request = new ForgotPasswordRequest();
        request.setEmail("student@example.com");

        when(authService.forgotPassword(any(ForgotPasswordRequest.class)))
            .thenReturn("If an account exists, an OTP has been sent.");

        mockMvc.perform(post("/auth/forgot-password")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.message").value("If an account exists, an OTP has been sent."));
    }

    @Test
    void resetPasswordReturnsSuccess() throws Exception {
        ResetPasswordRequest request = new ResetPasswordRequest();
        request.setEmail("student@example.com");
        request.setOtp("112233");
        request.setNewPassword("newpass1");

        doNothing().when(authService).resetPassword(any(ResetPasswordRequest.class));

        mockMvc.perform(post("/auth/reset-password")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.message").value("Password reset successful"));
    }

    @Test
    void loginBlockedWhenEmailNotVerified() throws Exception {
        LoginRequest request = new LoginRequest();
        request.setEmail("student@example.com");
        request.setPassword("secret12");

        when(authService.login(any(LoginRequest.class)))
            .thenThrow(new BadRequestException("Please verify your email using the OTP sent to your email."));

        mockMvc.perform(post("/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.message").value("Please verify your email using the OTP sent to your email."));
    }
}

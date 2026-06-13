package com.examsaathi.controller;

import com.examsaathi.dto.request.ForgotPasswordRequest;
import com.examsaathi.dto.request.GoogleSignInRequest;
import com.examsaathi.dto.request.LoginRequest;
import com.examsaathi.dto.request.RegisterRequest;
import com.examsaathi.dto.request.ResendEmailOtpRequest;
import com.examsaathi.dto.request.ResetPasswordRequest;
import com.examsaathi.dto.request.VerifyEmailOtpRequest;
import com.examsaathi.dto.request.VerifyForgotPasswordOtpRequest;
import com.examsaathi.dto.response.ApiResponse;
import com.examsaathi.dto.response.AuthResponse;
import com.examsaathi.dto.response.RegisterResponse;
import com.examsaathi.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
@Tag(name = "Authentication", description = "Register, login, OTP verification, and password reset APIs")
public class AuthController {

    private final AuthService authService;

    @PostMapping("/register")
    @Operation(summary = "Register a new student")
    public ResponseEntity<ApiResponse<RegisterResponse>> register(
            @Valid @RequestBody RegisterRequest request) {
        RegisterResponse response = authService.register(request);
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(ApiResponse.success(
                "Registration successful. Enter the OTP sent to your email to verify your account.",
                response));
    }

    @PostMapping("/login")
    @Operation(summary = "Login with email and password")
    public ResponseEntity<ApiResponse<AuthResponse>> login(
            @Valid @RequestBody LoginRequest request) {
        AuthResponse response = authService.login(request);
        return ResponseEntity.ok(ApiResponse.success("Login successful", response));
    }

    @PostMapping("/google")
    @Operation(summary = "Sign in with Google idToken")
    public ResponseEntity<ApiResponse<AuthResponse>> signInWithGoogle(
            @Valid @RequestBody GoogleSignInRequest request) {
        AuthResponse response = authService.signInWithGoogle(request);
        return ResponseEntity.ok(ApiResponse.success("Google sign-in successful", response));
    }

    @PostMapping("/verify-email-otp")
    @Operation(summary = "Verify email address using OTP")
    public ResponseEntity<ApiResponse<Void>> verifyEmailOtp(
            @Valid @RequestBody VerifyEmailOtpRequest request) {
        authService.verifyEmailOtp(request);
        return ResponseEntity.ok(ApiResponse.success("Email verified successfully", null));
    }

    @PostMapping("/resend-email-otp")
    @Operation(summary = "Resend email verification OTP")
    public ResponseEntity<ApiResponse<Void>> resendEmailOtp(
            @Valid @RequestBody ResendEmailOtpRequest request) {
        String message = authService.resendEmailOtp(request);
        return ResponseEntity.ok(ApiResponse.success(message, null));
    }

    @PostMapping("/forgot-password")
    @Operation(summary = "Request a password reset OTP")
    public ResponseEntity<ApiResponse<Void>> forgotPassword(
            @Valid @RequestBody ForgotPasswordRequest request) {
        String message = authService.forgotPassword(request);
        return ResponseEntity.ok(ApiResponse.success(message, null));
    }

    @PostMapping("/verify-forgot-password-otp")
    @Operation(summary = "Validate forgot-password OTP before reset")
    public ResponseEntity<ApiResponse<Void>> verifyForgotPasswordOtp(
            @Valid @RequestBody VerifyForgotPasswordOtpRequest request) {
        authService.verifyForgotPasswordOtp(request);
        return ResponseEntity.ok(ApiResponse.success("OTP verified successfully", null));
    }

    @PostMapping("/reset-password")
    @Operation(summary = "Reset password using OTP")
    public ResponseEntity<ApiResponse<Void>> resetPassword(
            @Valid @RequestBody ResetPasswordRequest request) {
        authService.resetPassword(request);
        return ResponseEntity.ok(ApiResponse.success("Password reset successful", null));
    }
}

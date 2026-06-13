package com.examsaathi.service;

import com.examsaathi.dto.request.ForgotPasswordRequest;
import com.examsaathi.dto.request.LoginRequest;
import com.examsaathi.dto.request.RegisterRequest;
import com.examsaathi.dto.request.ResendEmailOtpRequest;
import com.examsaathi.dto.request.ResetPasswordRequest;
import com.examsaathi.dto.request.VerifyEmailOtpRequest;
import com.examsaathi.dto.request.VerifyForgotPasswordOtpRequest;
import com.examsaathi.dto.response.RegisterResponse;
import com.examsaathi.entity.AuthProvider;
import com.examsaathi.entity.OtpPurpose;
import com.examsaathi.entity.Role;
import com.examsaathi.entity.User;
import com.examsaathi.exception.BadRequestException;
import com.examsaathi.repository.RoleRepository;
import com.examsaathi.repository.UserRepository;
import com.examsaathi.security.JwtTokenProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthServiceEmailTest {

    @Mock private UserRepository userRepository;
    @Mock private RoleRepository roleRepository;
    @Mock private PasswordEncoder passwordEncoder;
    @Mock private AuthenticationManager authenticationManager;
    @Mock private JwtTokenProvider tokenProvider;
    @Mock private GoogleTokenVerifier googleTokenVerifier;
    @Mock private EmailService emailService;
    @Mock private OtpService otpService;
    @Mock private Authentication authentication;

    private AuthService authService;
    private Role studentRole;

    @BeforeEach
    void setUp() {
        authService = new AuthService(
            userRepository,
            roleRepository,
            passwordEncoder,
            authenticationManager,
            tokenProvider,
            new UserMapper(),
            googleTokenVerifier,
            emailService,
            otpService
        );
        studentRole = Role.builder().id(1L).name(Role.RoleName.ROLE_STUDENT).build();
    }

    @Test
    void registerSendsVerificationOtp() {
        RegisterRequest request = new RegisterRequest();
        request.setFullName("Student");
        request.setEmail("student@example.com");
        request.setPassword("secret12");

        when(userRepository.existsByEmail(request.getEmail())).thenReturn(false);
        when(roleRepository.findByName(Role.RoleName.ROLE_STUDENT)).thenReturn(Optional.of(studentRole));
        when(passwordEncoder.encode(request.getPassword())).thenReturn("encoded-password");
        when(otpService.generateOtp()).thenReturn("483921");
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
            User user = invocation.getArgument(0);
            user.setId(10L);
            return user;
        });
        doNothing().when(otpService).issueOtp(any(User.class), any(OtpPurpose.class), any(String.class));
        doNothing().when(emailService).sendVerificationOtpEmail(any(User.class), any(String.class));

        RegisterResponse response = authService.register(request);

        verify(emailService).sendVerificationOtpEmail(any(User.class), eq("483921"));
        assertThat(response.getEmail()).isEqualTo("student@example.com");
    }

    @Test
    void verifyEmailOtpSucceeds() {
        VerifyEmailOtpRequest request = new VerifyEmailOtpRequest();
        request.setEmail("student@example.com");
        request.setOtp("483921");

        User user = otpUser("483921", OtpPurpose.VERIFY_EMAIL, false);
        when(userRepository.findByEmail("student@example.com")).thenReturn(Optional.of(user));
        when(userRepository.save(user)).thenReturn(user);
        doNothing().when(otpService).validateOtp(user, "483921", OtpPurpose.VERIFY_EMAIL);
        doNothing().when(otpService).clearOtp(user);

        authService.verifyEmailOtp(request);

        assertThat(user.getIsEmailVerified()).isTrue();
    }

    @Test
    void verifyEmailOtpFailsWhenExpired() {
        VerifyEmailOtpRequest request = new VerifyEmailOtpRequest();
        request.setEmail("student@example.com");
        request.setOtp("483921");

        User user = otpUser("483921", OtpPurpose.VERIFY_EMAIL, false);
        user.setEmailOtpExpiry(LocalDateTime.now().minusMinutes(1));
        when(userRepository.findByEmail("student@example.com")).thenReturn(Optional.of(user));
        org.mockito.Mockito.doThrow(new BadRequestException("OTP has expired"))
            .when(otpService).validateOtp(user, "483921", OtpPurpose.VERIFY_EMAIL);

        assertThatThrownBy(() -> authService.verifyEmailOtp(request))
            .isInstanceOf(BadRequestException.class)
            .hasMessage("OTP has expired");
    }

    @Test
    void forgotPasswordSendsResetOtp() {
        ForgotPasswordRequest request = new ForgotPasswordRequest();
        request.setEmail("student@example.com");

        User user = User.builder()
            .id(1L)
            .email("student@example.com")
            .authProvider(AuthProvider.EMAIL)
            .password("encoded")
            .build();

        when(userRepository.findByEmail("student@example.com")).thenReturn(Optional.of(user));
        when(otpService.generateOtp()).thenReturn("112233");
        when(userRepository.save(user)).thenReturn(user);
        doNothing().when(otpService).issueOtp(user, OtpPurpose.FORGOT_PASSWORD, "112233");
        doNothing().when(emailService).sendPasswordResetOtpEmail(user, "112233");

        String message = authService.forgotPassword(request);

        assertThat(message).contains("If an account exists");
        verify(emailService).sendPasswordResetOtpEmail(user, "112233");
    }

    @Test
    void resetPasswordSucceeds() {
        ResetPasswordRequest request = new ResetPasswordRequest();
        request.setEmail("student@example.com");
        request.setOtp("112233");
        request.setNewPassword("newpass1");

        User user = otpUser("112233", OtpPurpose.FORGOT_PASSWORD, true);
        when(userRepository.findByEmail("student@example.com")).thenReturn(Optional.of(user));
        when(passwordEncoder.encode("newpass1")).thenReturn("encoded-new");
        when(userRepository.save(user)).thenReturn(user);
        doNothing().when(otpService).validateOtp(user, "112233", OtpPurpose.FORGOT_PASSWORD);
        doNothing().when(otpService).clearOtp(user);

        authService.resetPassword(request);

        assertThat(user.getPassword()).isEqualTo("encoded-new");
    }

    @Test
    void loginBlockedWhenEmailNotVerified() {
        LoginRequest request = new LoginRequest();
        request.setEmail("student@example.com");
        request.setPassword("secret12");

        User user = User.builder()
            .id(1L)
            .email("student@example.com")
            .authProvider(AuthProvider.EMAIL)
            .isEmailVerified(false)
            .build();

        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
            .thenReturn(authentication);
        when(userRepository.findByEmailWithExam("student@example.com")).thenReturn(Optional.of(user));

        assertThatThrownBy(() -> authService.login(request))
            .isInstanceOf(BadRequestException.class)
            .hasMessage("Please verify your email using the OTP sent to your email.");
    }

    @Test
    void resendEmailOtpIssuesNewOtp() {
        ResendEmailOtpRequest request = new ResendEmailOtpRequest();
        request.setEmail("student@example.com");

        User user = User.builder()
            .id(1L)
            .email("student@example.com")
            .authProvider(AuthProvider.EMAIL)
            .isEmailVerified(false)
            .build();

        when(userRepository.findByEmail("student@example.com")).thenReturn(Optional.of(user));
        when(otpService.generateOtp()).thenReturn("999888");
        when(userRepository.save(user)).thenReturn(user);
        doNothing().when(otpService).issueOtp(user, OtpPurpose.VERIFY_EMAIL, "999888");
        doNothing().when(emailService).sendVerificationOtpEmail(user, "999888");

        String message = authService.resendEmailOtp(request);

        assertThat(message).contains("If an account exists");
        verify(emailService).sendVerificationOtpEmail(user, "999888");
    }

    private User otpUser(String otp, OtpPurpose purpose, boolean verified) {
        return User.builder()
            .id(1L)
            .email("student@example.com")
            .emailOtp(otp)
            .emailOtpExpiry(LocalDateTime.now().plusMinutes(5))
            .emailOtpAttempts(0)
            .otpPurpose(purpose)
            .isEmailVerified(verified)
            .build();
    }
}

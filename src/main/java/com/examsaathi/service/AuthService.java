package com.examsaathi.service;

import com.examsaathi.dto.request.ForgotPasswordRequest;
import com.examsaathi.dto.request.GoogleSignInRequest;
import com.examsaathi.dto.request.LoginRequest;
import com.examsaathi.dto.request.RegisterRequest;
import com.examsaathi.dto.request.ResendEmailOtpRequest;
import com.examsaathi.dto.request.ResetPasswordRequest;
import com.examsaathi.dto.request.VerifyEmailOtpRequest;
import com.examsaathi.dto.request.VerifyForgotPasswordOtpRequest;
import com.examsaathi.dto.response.AuthResponse;
import com.examsaathi.dto.response.RegisterResponse;
import com.examsaathi.entity.AuthProvider;
import com.examsaathi.entity.OtpPurpose;
import com.examsaathi.entity.Role;
import com.examsaathi.entity.User;
import com.examsaathi.exception.BadRequestException;
import com.examsaathi.exception.EmailDeliveryException;
import com.examsaathi.repository.RoleRepository;
import com.examsaathi.repository.UserRepository;
import com.examsaathi.security.JwtTokenProvider;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Set;

/**
 * Authentication service — register, login, OTP verification, and password reset.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AuthService {

    private static final String GENERIC_RESET_MESSAGE =
        "If an account exists, an OTP has been sent.";
    private static final String GENERIC_RESEND_MESSAGE =
        "If an account exists and is not yet verified, an OTP has been sent.";

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtTokenProvider tokenProvider;
    private final UserMapper userMapper;
    private final GoogleTokenVerifier googleTokenVerifier;
    private final EmailService emailService;
    private final OtpService otpService;

    @Transactional
    public RegisterResponse register(RegisterRequest request) {
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new BadRequestException("Email already registered: " + request.getEmail());
        }

        Role studentRole = roleRepository.findByName(Role.RoleName.ROLE_STUDENT)
            .orElseThrow(() -> new IllegalStateException("ROLE_STUDENT not found in DB. Run DataInitializer."));

        User user = User.builder()
            .fullName(request.getFullName())
            .email(request.getEmail())
            .password(passwordEncoder.encode(request.getPassword()))
            .phone(request.getPhone())
            .authProvider(AuthProvider.EMAIL)
            .roles(Set.of(studentRole))
            .build();

        issueAndSendOtp(user, OtpPurpose.VERIFY_EMAIL);
        userRepository.save(user);
        log.info("New student registered: userId={} email={}", user.getId(), user.getEmail());

        return RegisterResponse.builder()
            .email(user.getEmail())
            .fullName(user.getFullName())
            .emailVerificationRequired(true)
            .build();
    }

    @Transactional(readOnly = true)
    public AuthResponse login(LoginRequest request) {
        Authentication auth = authenticationManager.authenticate(
            new UsernamePasswordAuthenticationToken(request.getEmail(), request.getPassword())
        );

        User user = userRepository.findByEmailWithExam(request.getEmail())
            .orElseThrow();

        if (user.getAuthProvider() == AuthProvider.EMAIL && !Boolean.TRUE.equals(user.getIsEmailVerified())) {
            log.info("Login blocked for unverified email userId={}", user.getId());
            throw new BadRequestException("Please verify your email using the OTP sent to your email.");
        }

        String token = tokenProvider.generateToken(auth);
        return buildAuthResponse(token, user);
    }

    @Transactional
    public AuthResponse signInWithGoogle(GoogleSignInRequest request) {
        GoogleIdToken.Payload payload = googleTokenVerifier.verify(request.getIdToken());

        String googleId = payload.getSubject();
        String email = payload.getEmail();
        if (email == null || email.isBlank()) {
            throw new BadRequestException("Google account does not include an email address");
        }
        email = email.trim();

        String name = (String) payload.get("name");
        String picture = (String) payload.get("picture");
        boolean emailVerified = Boolean.TRUE.equals(payload.getEmailVerified());

        final String lookupEmail = email;
        User user = userRepository.findByGoogleId(googleId)
            .or(() -> userRepository.findByEmailWithExam(lookupEmail))
            .orElse(null);

        if (user != null) {
            if (user.getGoogleId() == null) {
                user.setGoogleId(googleId);
                log.info("Linked Google account to existing user: {}", email);
            }
            if (picture != null && !picture.isBlank()) {
                user.setAvatarUrl(picture);
            }
            if (emailVerified) {
                user.setIsEmailVerified(true);
            }
            userRepository.save(user);
        } else {
            Role studentRole = roleRepository.findByName(Role.RoleName.ROLE_STUDENT)
                .orElseThrow(() -> new IllegalStateException("ROLE_STUDENT not found in DB. Run DataInitializer."));

            user = User.builder()
                .email(email)
                .fullName(name != null && !name.isBlank() ? name : email.split("@")[0])
                .googleId(googleId)
                .avatarUrl(picture)
                .authProvider(AuthProvider.GOOGLE)
                .isEmailVerified(emailVerified)
                .roles(Set.of(studentRole))
                .build();
            userRepository.save(user);
            log.info("New Google student registered: {}", email);
        }

        user = userRepository.findByEmailWithExam(email).orElseThrow();
        String token = tokenProvider.generateToken(email);
        return buildAuthResponse(token, user);
    }

    @Transactional
    public void verifyEmailOtp(VerifyEmailOtpRequest request) {
        User user = userRepository.findByEmail(request.getEmail().trim())
            .orElseThrow(() -> new BadRequestException("Invalid or expired OTP"));

        try {
            otpService.validateOtp(user, request.getOtp(), OtpPurpose.VERIFY_EMAIL);
        } catch (BadRequestException ex) {
            userRepository.save(user);
            throw ex;
        }

        user.setIsEmailVerified(true);
        otpService.clearOtp(user);
        userRepository.save(user);
        log.info("Email verified via OTP for userId={} email={}", user.getId(), user.getEmail());
    }

    @Transactional
    public String resendEmailOtp(ResendEmailOtpRequest request) {
        userRepository.findByEmail(request.getEmail().trim())
            .filter(user -> user.getAuthProvider() == AuthProvider.EMAIL)
            .filter(user -> !Boolean.TRUE.equals(user.getIsEmailVerified()))
            .ifPresent(user -> {
                issueAndSendOtp(user, OtpPurpose.VERIFY_EMAIL);
                userRepository.save(user);
                log.info("Verification OTP resent for userId={}", user.getId());
            });

        return GENERIC_RESEND_MESSAGE;
    }

    @Transactional
    public String forgotPassword(ForgotPasswordRequest request) {
        userRepository.findByEmail(request.getEmail().trim())
            .filter(user -> user.getAuthProvider() == AuthProvider.EMAIL)
            .filter(user -> user.getPassword() != null)
            .ifPresent(user -> {
                issueAndSendOtp(user, OtpPurpose.FORGOT_PASSWORD);
                userRepository.save(user);
                log.info("Password reset OTP sent for userId={}", user.getId());
            });

        return GENERIC_RESET_MESSAGE;
    }

    @Transactional
    public void verifyForgotPasswordOtp(VerifyForgotPasswordOtpRequest request) {
        User user = userRepository.findByEmail(request.getEmail().trim())
            .orElseThrow(() -> new BadRequestException("Invalid or expired OTP"));

        try {
            otpService.validateOtp(user, request.getOtp(), OtpPurpose.FORGOT_PASSWORD);
        } catch (BadRequestException ex) {
            userRepository.save(user);
            throw ex;
        }
        userRepository.save(user);
        log.info("Forgot-password OTP validated for userId={}", user.getId());
    }

    @Transactional
    public void resetPassword(ResetPasswordRequest request) {
        User user = userRepository.findByEmail(request.getEmail().trim())
            .orElseThrow(() -> new BadRequestException("Invalid or expired OTP"));

        try {
            otpService.validateOtp(user, request.getOtp(), OtpPurpose.FORGOT_PASSWORD);
        } catch (BadRequestException ex) {
            userRepository.save(user);
            throw ex;
        }

        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        otpService.clearOtp(user);
        userRepository.save(user);
        log.info("Password reset completed for userId={}", user.getId());
    }

    private void issueAndSendOtp(User user, OtpPurpose purpose) {
        String otp = otpService.generateOtp();
        otpService.issueOtp(user, purpose, otp);
        try {
            if (purpose == OtpPurpose.VERIFY_EMAIL) {
                emailService.sendVerificationOtpEmail(user, otp);
            } else {
                emailService.sendPasswordResetOtpEmail(user, otp);
            }
        } catch (EmailDeliveryException ex) {
            log.error("Failed to send OTP email for userId={} purpose={}: {}",
                user.getId(), purpose, ex.getMessage());
        }
    }

    private AuthResponse buildAuthResponse(String token, User user) {
        return AuthResponse.builder()
            .accessToken(token)
            .tokenType("Bearer")
            .expiresIn(tokenProvider.getExpirationMs())
            .user(userMapper.toResponse(user))
            .build();
    }
}

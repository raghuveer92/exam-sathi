package com.examsaathi.service;

import com.examsaathi.entity.OtpPurpose;
import com.examsaathi.entity.User;
import com.examsaathi.exception.BadRequestException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class OtpServiceTest {

    private OtpService otpService;

    @BeforeEach
    void setUp() {
        otpService = new OtpService();
    }

    @Test
    void generateOtpReturnsSixDigitCode() {
        String otp = otpService.generateOtp();
        assertThat(otp).matches("\\d{6}");
    }

    @Test
    void validateOtpSucceedsForMatchingCode() {
        User user = activeUser("123456", OtpPurpose.VERIFY_EMAIL);
        otpService.validateOtp(user, "123456", OtpPurpose.VERIFY_EMAIL);
    }

    @Test
    void validateOtpFailsWhenExpired() {
        User user = User.builder()
            .emailOtp("123456")
            .emailOtpExpiry(LocalDateTime.now().minusMinutes(1))
            .emailOtpAttempts(0)
            .otpPurpose(OtpPurpose.VERIFY_EMAIL)
            .build();

        assertThatThrownBy(() -> otpService.validateOtp(user, "123456", OtpPurpose.VERIFY_EMAIL))
            .isInstanceOf(BadRequestException.class)
            .hasMessage("OTP has expired");
    }

    @Test
    void validateOtpIncrementsAttemptsOnMismatch() {
        User user = activeUser("123456", OtpPurpose.VERIFY_EMAIL);

        assertThatThrownBy(() -> otpService.validateOtp(user, "000000", OtpPurpose.VERIFY_EMAIL))
            .isInstanceOf(BadRequestException.class)
            .hasMessage("Invalid OTP");

        assertThat(user.getEmailOtpAttempts()).isEqualTo(1);
    }

    @Test
    void issueOtpEnforcesHourlySendLimit() {
        User user = User.builder()
            .otpSendCount(3)
            .otpSendWindowStart(LocalDateTime.now().minusMinutes(10))
            .build();

        assertThatThrownBy(() -> otpService.issueOtp(user, OtpPurpose.VERIFY_EMAIL, "111111"))
            .isInstanceOf(BadRequestException.class)
            .hasMessageContaining("Too many OTP requests");
    }

    private User activeUser(String otp, OtpPurpose purpose) {
        return User.builder()
            .emailOtp(otp)
            .emailOtpExpiry(LocalDateTime.now().plusMinutes(5))
            .emailOtpAttempts(0)
            .otpPurpose(purpose)
            .build();
    }
}

package com.examsaathi.service;

import com.examsaathi.entity.OtpPurpose;
import com.examsaathi.entity.User;
import com.examsaathi.exception.BadRequestException;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

@Service
public class OtpService {

    public static final int OTP_EXPIRY_MINUTES = 10;
    public static final int MAX_VERIFY_ATTEMPTS = 5;
    public static final int MAX_SENDS_PER_HOUR = 3;

    private final SecureRandom secureRandom = new SecureRandom();

    public String generateOtp() {
        int otp = secureRandom.nextInt(900_000) + 100_000;
        return String.valueOf(otp);
    }

    public void issueOtp(User user, OtpPurpose purpose, String otp) {
        enforceSendRateLimit(user);

        user.setEmailOtp(otp);
        user.setEmailOtpExpiry(LocalDateTime.now().plusMinutes(OTP_EXPIRY_MINUTES));
        user.setEmailOtpAttempts(0);
        user.setOtpPurpose(purpose);
    }

    public void validateOtp(User user, String submittedOtp, OtpPurpose expectedPurpose) {
        if (submittedOtp == null || submittedOtp.isBlank()) {
            throw new BadRequestException("OTP is required");
        }

        if (user.getOtpPurpose() != expectedPurpose) {
            throw new BadRequestException("Invalid or expired OTP");
        }

        if (user.getEmailOtp() == null || user.getEmailOtpExpiry() == null) {
            throw new BadRequestException("Invalid or expired OTP");
        }

        if (user.getEmailOtpExpiry().isBefore(LocalDateTime.now())) {
            throw new BadRequestException("OTP has expired");
        }

        if (user.getEmailOtpAttempts() != null && user.getEmailOtpAttempts() >= MAX_VERIFY_ATTEMPTS) {
            throw new BadRequestException("Too many invalid OTP attempts. Please request a new OTP.");
        }

        if (!user.getEmailOtp().equals(submittedOtp.trim())) {
            user.setEmailOtpAttempts((user.getEmailOtpAttempts() == null ? 0 : user.getEmailOtpAttempts()) + 1);
            throw new BadRequestException("Invalid OTP");
        }
    }

    public void clearOtp(User user) {
        user.setEmailOtp(null);
        user.setEmailOtpExpiry(null);
        user.setEmailOtpAttempts(0);
        user.setOtpPurpose(null);
    }

    private void enforceSendRateLimit(User user) {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime windowStart = user.getOtpSendWindowStart();

        if (windowStart == null || ChronoUnit.HOURS.between(windowStart, now) >= 1) {
            user.setOtpSendWindowStart(now);
            user.setOtpSendCount(0);
        }

        int sendCount = user.getOtpSendCount() == null ? 0 : user.getOtpSendCount();
        if (sendCount >= MAX_SENDS_PER_HOUR) {
            throw new BadRequestException("Too many OTP requests. Please try again in an hour.");
        }

        user.setOtpSendCount(sendCount + 1);
    }
}

package com.examsaathi.util;

import com.examsaathi.entity.AuthProvider;
import com.examsaathi.entity.User;
import com.examsaathi.exception.BadRequestException;

public final class EmailVerificationGuard {

    private EmailVerificationGuard() {}

    public static void requireVerifiedForExamSetup(User user) {
        if (user.getAuthProvider() == AuthProvider.EMAIL
            && !Boolean.TRUE.equals(user.getIsEmailVerified())) {
            throw new BadRequestException("Please verify your email before adding an exam.");
        }
    }
}

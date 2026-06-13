package com.examsaathi.service;

import com.examsaathi.config.BrevoProperties;
import com.examsaathi.entity.User;
import com.examsaathi.exception.EmailDeliveryException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class EmailService {

    private static final String BREVO_SEND_URL = "https://api.brevo.com/v3/smtp/email";

    private final RestTemplate brevoRestTemplate;
    private final BrevoProperties brevoProperties;
    private final EmailTemplateService emailTemplateService;

    public void sendVerificationOtpEmail(User user, String otp) {
        String html = emailTemplateService.buildVerificationOtpEmail(user.getFullName(), otp);
        sendEmail(user.getEmail(), "Verify Your ExamSaathi Account", html);
        log.info("Verification OTP email sent to userId={} email={}", user.getId(), maskEmail(user.getEmail()));
    }

    public void sendPasswordResetOtpEmail(User user, String otp) {
        String html = emailTemplateService.buildPasswordResetOtpEmail(user.getFullName(), otp);
        sendEmail(user.getEmail(), "Reset Your ExamSaathi Password", html);
        log.info("Password reset OTP email sent to userId={} email={}", user.getId(), maskEmail(user.getEmail()));
    }

    public void sendEmail(String to, String subject, String htmlContent) {
        if (!brevoProperties.isConfigured()) {
            log.warn("Brevo API key not configured; skipping email to {}", maskEmail(to));
            return;
        }

        Map<String, Object> payload = Map.of(
            "sender", Map.of(
                "name", brevoProperties.getSenderName(),
                "email", brevoProperties.getSenderEmail()
            ),
            "to", List.of(Map.of("email", to)),
            "subject", subject,
            "htmlContent", htmlContent
        );

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("api-key", brevoProperties.getApiKey());

        HttpEntity<Map<String, Object>> request = new HttpEntity<>(payload, headers);
        executeWithRetry(request);
    }

    private void executeWithRetry(HttpEntity<Map<String, Object>> request) {
        int maxAttempts = brevoProperties.getMaxRetries();
        long delayMs = brevoProperties.getRetryDelayMs();

        for (int attempt = 1; attempt <= maxAttempts; attempt++) {
            try {
                ResponseEntity<String> response = brevoRestTemplate.postForEntity(
                    BREVO_SEND_URL, request, String.class);

                if (response.getStatusCode().is2xxSuccessful()) {
                    log.debug("Brevo email accepted with status {}", response.getStatusCode());
                    return;
                }

                boolean retryable = response.getStatusCode().is5xxServerError()
                    || response.getStatusCode() == HttpStatus.TOO_MANY_REQUESTS;

                if (!retryable || attempt == maxAttempts) {
                    throw new EmailDeliveryException(
                        "Brevo API returned status " + response.getStatusCode(), retryable);
                }

                log.warn("Brevo API attempt {}/{} failed with status {}; retrying",
                    attempt, maxAttempts, response.getStatusCode());
            } catch (ResourceAccessException ex) {
                if (attempt == maxAttempts) {
                    throw new EmailDeliveryException(
                        "Brevo API request timed out or failed to connect", ex, true);
                }
                log.warn("Brevo API attempt {}/{} failed due to network error: {}; retrying",
                    attempt, maxAttempts, ex.getMessage());
            } catch (HttpStatusCodeException ex) {
                boolean retryable = ex.getStatusCode().is5xxServerError()
                    || ex.getStatusCode() == HttpStatus.TOO_MANY_REQUESTS;

                if (!retryable || attempt == maxAttempts) {
                    throw new EmailDeliveryException(
                        "Brevo API error: " + ex.getStatusCode() + " " + ex.getResponseBodyAsString(),
                        ex, retryable);
                }

                log.warn("Brevo API attempt {}/{} failed with {} {}; retrying",
                    attempt, maxAttempts, ex.getStatusCode(), ex.getResponseBodyAsString());
            }

            sleep(delayMs * attempt);
        }
    }

    private void sleep(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new EmailDeliveryException("Email delivery interrupted", ex, true);
        }
    }

    private String maskEmail(String email) {
        if (email == null || !email.contains("@")) {
            return "***";
        }
        int at = email.indexOf('@');
        String local = email.substring(0, at);
        String domain = email.substring(at);
        if (local.length() <= 2) {
            return "**" + domain;
        }
        return local.charAt(0) + "***" + local.charAt(local.length() - 1) + domain;
    }
}

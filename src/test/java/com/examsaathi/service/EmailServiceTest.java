package com.examsaathi.service;

import com.examsaathi.config.BrevoProperties;
import com.examsaathi.entity.User;
import com.examsaathi.exception.EmailDeliveryException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EmailServiceTest {

    @Mock
    private RestTemplate brevoRestTemplate;

    private BrevoProperties brevoProperties;
    private EmailService emailService;

    @BeforeEach
    void setUp() {
        brevoProperties = new BrevoProperties();
        brevoProperties.setApiKey("test-api-key");
        brevoProperties.setSenderName("Exam Saathi");
        brevoProperties.setSenderEmail("examsathi5@gmail.com");
        brevoProperties.setMaxRetries(3);
        brevoProperties.setRetryDelayMs(1L);

        emailService = new EmailService(brevoRestTemplate, brevoProperties, new EmailTemplateService());
    }

    @Test
    void sendVerificationOtpEmailIncludesOtpInHtml() {
        User user = User.builder()
            .id(1L)
            .email("student@example.com")
            .fullName("Student")
            .build();

        when(brevoRestTemplate.postForEntity(any(String.class), any(HttpEntity.class), eq(String.class)))
            .thenReturn(ResponseEntity.ok("accepted"));

        emailService.sendVerificationOtpEmail(user, "483921");

        ArgumentCaptor<HttpEntity<Map<String, Object>>> captor = ArgumentCaptor.forClass(HttpEntity.class);
        verify(brevoRestTemplate).postForEntity(eq("https://api.brevo.com/v3/smtp/email"), captor.capture(), eq(String.class));

        assertThat(captor.getValue().getBody().get("subject")).isEqualTo("Verify Your ExamSaathi Account");
        assertThat(captor.getValue().getBody().get("htmlContent").toString()).contains("483921");
    }

    @Test
    void sendPasswordResetOtpEmailIncludesOtpInHtml() {
        User user = User.builder()
            .id(2L)
            .email("student@example.com")
            .fullName("Student")
            .build();

        when(brevoRestTemplate.postForEntity(any(String.class), any(HttpEntity.class), eq(String.class)))
            .thenReturn(ResponseEntity.ok("accepted"));

        emailService.sendPasswordResetOtpEmail(user, "112233");

        ArgumentCaptor<HttpEntity<Map<String, Object>>> captor = ArgumentCaptor.forClass(HttpEntity.class);
        verify(brevoRestTemplate).postForEntity(eq("https://api.brevo.com/v3/smtp/email"), captor.capture(), eq(String.class));

        assertThat(captor.getValue().getBody().get("subject")).isEqualTo("Reset Your ExamSaathi Password");
        assertThat(captor.getValue().getBody().get("htmlContent").toString()).contains("112233");
    }

    @Test
    void sendEmailRetriesOnTransientFailure() {
        when(brevoRestTemplate.postForEntity(any(String.class), any(HttpEntity.class), eq(String.class)))
            .thenThrow(new HttpServerErrorException(HttpStatus.BAD_GATEWAY))
            .thenReturn(ResponseEntity.ok("accepted"));

        emailService.sendEmail("student@example.com", "Subject", "<p>Body</p>");

        verify(brevoRestTemplate, times(2)).postForEntity(any(String.class), any(HttpEntity.class), eq(String.class));
    }

    @Test
    void sendEmailSkipsWhenApiKeyMissing() {
        brevoProperties.setApiKey("");

        emailService.sendEmail("student@example.com", "Subject", "<p>Body</p>");

        verify(brevoRestTemplate, times(0)).postForEntity(any(String.class), any(HttpEntity.class), eq(String.class));
    }
}

package com.examsaathi.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "brevo")
@Getter
@Setter
public class BrevoProperties {

    private String apiKey = "";
    private String senderName = "Exam Saathi";
    private String senderEmail = "examsathi5@gmail.com";
    private String verificationBaseUrl = "https://api.examsaathi.com/api";
    private String passwordResetBaseUrl = "https://app.examsaathi.com";
    private int connectTimeoutMs = 5000;
    private int readTimeoutMs = 10000;
    private int maxRetries = 3;
    private long retryDelayMs = 1000;

    public boolean isConfigured() {
        return apiKey != null && !apiKey.isBlank();
    }
}

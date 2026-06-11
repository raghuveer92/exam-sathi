package com.examsaathi.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.List;

@Component
@ConfigurationProperties(prefix = "google.oauth")
@Getter
@Setter
public class GoogleOAuthProperties {

    /** Comma-separated OAuth client IDs (Web + Android) accepted as token audience. */
    private String clientIds = "";

    public List<String> acceptedClientIds() {
        if (clientIds == null || clientIds.isBlank()) {
            return List.of();
        }
        return Arrays.stream(clientIds.split(","))
            .map(String::trim)
            .filter(s -> !s.isEmpty())
            .toList();
    }
}

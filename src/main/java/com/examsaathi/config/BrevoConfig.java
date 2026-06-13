package com.examsaathi.config;

import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;

@Configuration
public class BrevoConfig {

    @Bean
    public RestTemplate brevoRestTemplate(BrevoProperties brevoProperties, RestTemplateBuilder builder) {
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(brevoProperties.getConnectTimeoutMs());
        requestFactory.setReadTimeout(brevoProperties.getReadTimeoutMs());

        return builder
            .requestFactory(() -> requestFactory)
            .build();
    }
}

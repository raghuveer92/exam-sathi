package com.examsaathi.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;

import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

@Configuration
@EnableAsync
public class AsyncConfig {

    @Bean(name = "syncBundleExecutor")
    public Executor syncBundleExecutor() {
        return Executors.newFixedThreadPool(4);
    }
}

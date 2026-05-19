package com.examsaathi;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.info.Contact;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.info.License;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

/**
 * ExamSaathi Backend Application
 * Study Tracking and Syllabus Management Platform
 */
@SpringBootApplication
@EnableJpaAuditing
@OpenAPIDefinition(
    info = @Info(
        title = "ExamSaathi API",
        version = "1.0.0",
        description = "REST API for ExamSaathi - A modern study tracking and syllabus management platform for exam preparation (CBSE, NEET, JEE, UPSC, SSC)",
        contact = @Contact(name = "ExamSaathi Team", email = "support@examsaathi.com"),
        license = @License(name = "Proprietary")
    )
)
public class ExamSaathiApplication {

    public static void main(String[] args) {
        SpringApplication.run(ExamSaathiApplication.class, args);
    }
}

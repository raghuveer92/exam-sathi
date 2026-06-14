package com.examsaathi.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@ConfigurationProperties(prefix = "app.google-sheets")
@Data
public class GoogleSheetsProperties {

    /** Minimum questions required in sheet pool before a topic test can start */
    private int minQuestionsPerTopic = 1;

    private int connectTimeoutMs = 10000;
    private int readTimeoutMs = 30000;

    private List<String> requiredColumns = List.of(
        "id", "exam", "subject", "topic", "question_en", "correct_option"
    );

    private List<String> optionalIdColumns = List.of("exam_id", "subject_id", "topic_id");
}

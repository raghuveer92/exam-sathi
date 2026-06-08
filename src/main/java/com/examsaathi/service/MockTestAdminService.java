package com.examsaathi.service;

import com.examsaathi.dto.response.MockTestPurgeResponse;
import com.examsaathi.repository.QuestionRepository;
import com.examsaathi.repository.TestAttemptAnswerRepository;
import com.examsaathi.repository.TestAttemptRepository;
import com.examsaathi.repository.TopicTestConfigRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class MockTestAdminService {

    private final TestAttemptAnswerRepository testAttemptAnswerRepository;
    private final TestAttemptRepository testAttemptRepository;
    private final QuestionRepository questionRepository;
    private final TopicTestConfigRepository topicTestConfigRepository;
    private final JdbcTemplate jdbcTemplate;

    @Transactional
    public MockTestPurgeResponse purgeAll() {
        long answerCount = testAttemptAnswerRepository.count();
        long attemptCount = testAttemptRepository.count();
        long questionCount = questionRepository.count();
        long configCount = topicTestConfigRepository.count();

        jdbcTemplate.execute("DELETE FROM test_attempt_answers");
        jdbcTemplate.execute("DELETE FROM test_attempts");
        jdbcTemplate.execute("DELETE FROM question_options");
        jdbcTemplate.execute("DELETE FROM questions");
        jdbcTemplate.execute("DELETE FROM topic_test_configs");

        return MockTestPurgeResponse.builder()
            .attemptAnswersDeleted(answerCount)
            .attemptsDeleted(attemptCount)
            .questionsDeleted(questionCount)
            .topicTestConfigsDeleted(configCount)
            .build();
    }
}

package com.examsaathi.dto.response;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class MockTestPurgeResponse {
    long attemptAnswersDeleted;
    long attemptsDeleted;
    long questionsDeleted;
    long topicTestConfigsDeleted;
}

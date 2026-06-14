package com.examsaathi.util;

import com.examsaathi.entity.TestAttempt;
import com.examsaathi.util.MockTestMasteryUtil;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class MockTestMasteryUtilTest {

    @Test
    void resolvesMasteryLevels() {
        assertThat(MockTestMasteryUtil.resolveMasteryLevel(30)).isEqualTo(TestAttempt.MasteryLevel.BEGINNER);
        assertThat(MockTestMasteryUtil.resolveMasteryLevel(40)).isEqualTo(TestAttempt.MasteryLevel.BEGINNER);
        assertThat(MockTestMasteryUtil.resolveMasteryLevel(50)).isEqualTo(TestAttempt.MasteryLevel.DEVELOPING);
        assertThat(MockTestMasteryUtil.resolveMasteryLevel(70)).isEqualTo(TestAttempt.MasteryLevel.PROFICIENT);
        assertThat(MockTestMasteryUtil.resolveMasteryLevel(90)).isEqualTo(TestAttempt.MasteryLevel.MASTERED);
    }

    @Test
    void labelsMatchIgnoresCaseAndSpaces() {
        assertThat(MockTestMasteryUtil.labelsMatch("SSC CGL", "ssc cgl")).isTrue();
        assertThat(MockTestMasteryUtil.labelsMatch("Coding  Decoding", "coding decoding")).isTrue();
    }
}

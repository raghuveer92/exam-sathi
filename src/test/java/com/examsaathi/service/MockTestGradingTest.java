package com.examsaathi.service;

import com.examsaathi.entity.Question;
import com.examsaathi.entity.QuestionOption;
import com.examsaathi.entity.Topic;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class MockTestGradingTest {

    @Test
    void gradesSingleCorrectAnswer() throws Exception {
        Question question = Question.builder()
            .questionType(Question.QuestionType.SINGLE_CORRECT)
            .marks(2.0)
            .negativeMarks(0.5)
            .difficultyLevel(Topic.DifficultyLevel.MEDIUM)
            .build();
        question.getOptions().add(option("A", true));
        question.getOptions().add(option("B", false));

        MockTestService service = new MockTestService(null, null, null, null, null, null);
        Method grade = MockTestService.class.getDeclaredMethod("gradeAnswer", Question.class, String.class);
        grade.setAccessible(true);

        assertThat(grade.invoke(service, question, "A")).isEqualTo(true);
        assertThat(grade.invoke(service, question, "B")).isEqualTo(false);
    }

    @Test
    void gradesMultipleCorrectAnswer() throws Exception {
        Question question = Question.builder()
            .questionType(Question.QuestionType.MULTIPLE_CORRECT)
            .marks(4.0)
            .negativeMarks(1.0)
            .difficultyLevel(Topic.DifficultyLevel.HARD)
            .build();
        question.getOptions().add(option("A", true));
        question.getOptions().add(option("B", true));
        question.getOptions().add(option("C", false));

        MockTestService service = new MockTestService(null, null, null, null, null, null);
        Method grade = MockTestService.class.getDeclaredMethod("gradeAnswer", Question.class, String.class);
        grade.setAccessible(true);

        assertThat(grade.invoke(service, question, "A,B")).isEqualTo(true);
        assertThat(grade.invoke(service, question, "A")).isEqualTo(false);
    }

    private QuestionOption option(String key, boolean correct) {
        return QuestionOption.builder()
            .optionKey(key)
            .optionText(key)
            .isCorrect(correct)
            .build();
    }
}

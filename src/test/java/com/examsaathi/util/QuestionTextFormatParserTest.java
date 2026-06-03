package com.examsaathi.util;

import com.examsaathi.exception.BadRequestException;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class QuestionTextFormatParserTest {

    @Test
    void parsesSingleCorrectWithCommas() {
        String text = """
            QUESTION: What is the average of 10, 20 and 30?
            TYPE: SINGLE_CORRECT
            OPTION_A: 15
            OPTION_B: 20
            OPTION_C: 25
            OPTION_D: 30
            CORRECT: B
            EXPLANATION: Average = (10 + 20 + 30) / 3 = 20
            MARKS: 1
            NEGATIVE_MARKS: 0.25
            PREVIOUS_YEAR: false
            """;

        var parsed = QuestionTextFormatParser.parse(text).get(0);
        assertThat(parsed.fields().get("QUESTION")).contains("10, 20 and 30");
        assertThat(parsed.fields().get("CORRECT")).isEqualTo("B");
        assertThat(parsed.options()).hasSize(4);
    }

    @Test
    void parsesMultipleCorrectWithPipeCorrect() {
        String text = """
            QUESTION: Which are prime?
            TYPE: MULTIPLE_CORRECT
            OPTION_A: 2
            OPTION_B: 3
            OPTION_C: 4
            OPTION_D: 5
            CORRECT: A|B|D
            EXPLANATION: 2, 3 and 5 are prime.
            MARKS: 2
            NEGATIVE_MARKS: 0.5
            PREVIOUS_YEAR: true
            PREVIOUS_YEAR_VALUE: 2023
            """;

        var parsed = QuestionTextFormatParser.parse(text).get(0);
        assertThat(parsed.fields().get("TYPE")).isEqualTo("MULTIPLE_CORRECT");
        assertThat(parsed.fields().get("PREVIOUS_YEAR_VALUE")).isEqualTo("2023");
    }

    @Test
    void parsesMultipleBlocksSeparatedByDashes() {
        String text = """
            QUESTION: Q1
            TYPE: SINGLE_CORRECT
            OPTION_A: Yes
            OPTION_B: No
            CORRECT: A
            MARKS: 1
            NEGATIVE_MARKS: 0

            ---

            QUESTION: Q2
            TYPE: SINGLE_CORRECT
            OPTION_A: 50%
            OPTION_B: 100%
            CORRECT: B
            MARKS: 1
            NEGATIVE_MARKS: 0
            """;

        assertThat(QuestionTextFormatParser.parse(text)).hasSize(2);
    }

    @Test
    void parseReplaceContentTreatsSeparatorsOnlyAsEmpty() {
        assertThat(QuestionTextFormatParser.parseReplaceContent("---\n\n---")).isEmpty();
        assertThat(QuestionTextFormatParser.parseReplaceContent("")).isEmpty();
    }

    @Test
    void rejectsMissingQuestion() {
        assertThatThrownBy(() -> QuestionTextFormatParser.parse("""
            TYPE: SINGLE_CORRECT
            OPTION_A: A
            OPTION_B: B
            CORRECT: A
            MARKS: 1
            NEGATIVE_MARKS: 0
            """)).isInstanceOf(BadRequestException.class);
    }
}

package com.examsaathi.service;

import com.examsaathi.config.CacheNames;
import com.examsaathi.dto.sheet.SheetQuestion;
import com.examsaathi.entity.Exam;
import com.examsaathi.entity.Subject;
import com.examsaathi.entity.Topic;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.cache.CacheManager;
import org.springframework.cache.concurrent.ConcurrentMapCacheManager;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GoogleSheetsServiceTest {

    @Mock
    private GoogleSheetsQuestionLoader loader;

  private GoogleSheetsService service;

    @BeforeEach
    void setUp() {
        com.examsaathi.config.GoogleSheetsProperties properties =
            new com.examsaathi.config.GoogleSheetsProperties();
        properties.setMinQuestionsPerTopic(2);
        CacheManager cacheManager = new ConcurrentMapCacheManager(CacheNames.SHEET_QUESTIONS);
        service = new GoogleSheetsService(loader, properties, cacheManager);
    }

    @Test
    void prefersIdBasedMatchingWhenTopicIdPresent() {
        Exam exam = Exam.builder().id(1L).name("SSC CGL").build();
        Subject subject = Subject.builder().id(10L).name("Maths").build();
        Topic topic = Topic.builder().id(100L).title("Percentage").build();

        List<SheetQuestion> questions = List.of(
            SheetQuestion.builder()
                .id("1")
                .examId("1")
                .subjectId("10")
                .topicId("100")
                .topic("Other label")
                .correctOption("A")
                .build(),
            SheetQuestion.builder()
                .id("2")
                .exam("SSC CGL")
                .subject("Maths")
                .topic("Percentage")
                .correctOption("B")
                .build()
        );

        List<SheetQuestion> matched = service.filterForTopic(questions, exam, subject, topic);
        assertThat(matched).hasSize(1);
        assertThat(matched.get(0).getId()).isEqualTo("1");
    }

    @Test
    void fallsBackToNameMatchingWhenIdColumnsAbsent() {
        Exam exam = Exam.builder().id(1L).name("SSC CGL").build();
        Subject subject = Subject.builder().id(10L).name("Maths").build();
        Topic topic = Topic.builder().id(100L).title("Percentage").build();

        List<SheetQuestion> questions = List.of(
            SheetQuestion.builder()
                .id("2")
                .exam("SSC CGL")
                .subject("Maths")
                .topic("Percentage")
                .correctOption("B")
                .build()
        );

        List<SheetQuestion> matched = service.filterForTopic(questions, exam, subject, topic);
        assertThat(matched).hasSize(1);
        assertThat(matched.get(0).getId()).isEqualTo("2");
    }

    @Test
    void resolvesEffectiveQuestionCountForSmallPools() {
        com.examsaathi.config.GoogleSheetsProperties properties =
            new com.examsaathi.config.GoogleSheetsProperties();
        properties.setMinQuestionsPerTopic(1);
        GoogleSheetsService service = new GoogleSheetsService(
            loader, properties, new org.springframework.cache.concurrent.ConcurrentMapCacheManager());

        assertThat(service.resolveEffectiveQuestionCount(10, 1, true)).isEqualTo(1);
        assertThat(service.canStartTest(10, 1, true, true)).isTrue();
    }
}

package com.examsaathi.service;

import com.examsaathi.dto.response.SyncCatalogResponse;
import com.examsaathi.entity.*;
import com.examsaathi.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SyncServiceTest {

    @Mock private ExamCategoryRepository categoryRepository;
    @Mock private ExamRepository examRepository;
    @Mock private SubjectRepository subjectRepository;
    @Mock private ChapterRepository chapterRepository;
    @Mock private TopicRepository topicRepository;
    @Mock private QuestionRepository questionRepository;
    @Mock private StudyProgressRepository studyProgressRepository;
    @Mock private UserExamRepository userExamRepository;
    @Mock private UserRepository userRepository;
    @Mock private DashboardService dashboardService;
    @Mock private StudyProgressService studyProgressService;
    @Mock private UserMapper mapper;
    @Mock private com.fasterxml.jackson.databind.ObjectMapper objectMapper;

    private SyncService syncService;

    @BeforeEach
    void setUp() {
        syncService = new SyncService(
            categoryRepository,
            examRepository,
            subjectRepository,
            chapterRepository,
            topicRepository,
            questionRepository,
            studyProgressRepository,
            userExamRepository,
            userRepository,
            dashboardService,
            studyProgressService,
            mapper,
            objectMapper
        );
    }

    @Test
    void getCatalogSync_scopedToExam_usesExamQueriesNotFullTableScans() {
        ExamCategory category = ExamCategory.builder().id(1L).name("Govt").displayOrder(1).isActive(true).build();
        Exam exam = Exam.builder().id(28L).name("REET").category(category).isActive(true).displayOrder(1).build();
        Subject subject = Subject.builder().id(87L).name("Child Dev").isActive(true).build();
        Chapter chapter = Chapter.builder().id(10L).subject(subject).title("Ch1").orderIndex(1).isActive(true).build();
        Topic topic = Topic.builder().id(100L).chapter(chapter).title("T1").isActive(true).build();

        when(examRepository.findActiveByIdInWithCategory(List.of(28L))).thenReturn(List.of(exam));
        when(subjectRepository.findActiveByExamIdOrderByDisplayOrderAsc(28L)).thenReturn(List.of(subject));
        when(topicRepository.findByExamId(28L)).thenReturn(List.of(topic));
        when(chapterRepository.findBySubjectIdInAndIsActiveTrueOrderByOrderIndexAsc(List.of(87L)))
            .thenReturn(List.of(chapter));
        when(categoryRepository.findAllById(any())).thenReturn(List.of(category));
        when(questionRepository.findReadyMockTestTopicIds()).thenReturn(List.of());
        when(mapper.toExamResponse(exam, false)).thenReturn(
            com.examsaathi.dto.response.ExamResponse.builder().id(28L).name("REET").isActive(true).build());
        when(mapper.toSubjectResponse(subject, false)).thenReturn(
            com.examsaathi.dto.response.SubjectResponse.builder().id(87L).name("Child Dev").isActive(true).build());
        when(mapper.toChapterResponse(chapter, false)).thenReturn(
            com.examsaathi.dto.response.ChapterResponse.builder().id(10L).subjectId(87L).title("Ch1").isActive(true).build());
        when(mapper.toTopicResponse(topic)).thenReturn(
            com.examsaathi.dto.response.TopicResponse.builder().id(100L).title("T1").isActive(true).build());

        SyncCatalogResponse response = syncService.getCatalogSync(null, List.of(28L));

        assertThat(response.getExams()).hasSize(1);
        assertThat(response.getSubjects()).hasSize(1);
        assertThat(response.getTopics()).hasSize(1);
        verify(topicRepository).findByExamId(28L);
    }
}

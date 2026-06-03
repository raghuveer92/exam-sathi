package com.examsaathi.service;

import com.examsaathi.dto.request.SubjectGroupSelectionRequest;
import com.examsaathi.dto.response.ExamSubjectGroupResponse;
import com.examsaathi.dto.response.SubjectResponse;
import com.examsaathi.entity.Exam;
import com.examsaathi.entity.ExamSubject;
import com.examsaathi.entity.ExamSubjectGroup;
import com.examsaathi.entity.ExamSubjectGroupItem;
import com.examsaathi.entity.Subject;
import com.examsaathi.entity.UserExam;
import com.examsaathi.entity.UserExamSubjectSelection;
import com.examsaathi.exception.BadRequestException;
import com.examsaathi.repository.ExamRepository;
import com.examsaathi.repository.ExamSubjectGroupItemRepository;
import com.examsaathi.repository.ExamSubjectGroupRepository;
import com.examsaathi.repository.ExamSubjectRepository;
import com.examsaathi.repository.SubjectRepository;
import com.examsaathi.repository.UserExamRepository;
import com.examsaathi.repository.UserExamSubjectSelectionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ExamSubjectGroupServiceTest {

    @Mock
    private ExamRepository examRepository;

    @Mock
    private SubjectRepository subjectRepository;

    @Mock
    private UserExamRepository userExamRepository;

    @Mock
    private ExamSubjectRepository examSubjectRepository;

    @Mock
    private ExamSubjectGroupRepository examSubjectGroupRepository;

    @Mock
    private ExamSubjectGroupItemRepository examSubjectGroupItemRepository;

    @Mock
    private UserExamSubjectSelectionRepository userExamSubjectSelectionRepository;

    private ExamSubjectGroupService service;

    private Exam exam;
    private UserExam userExam;
    private Subject math;
    private Subject biology;
    private Subject computerScience;
    private ExamSubject mathExamSubject;
    private ExamSubject biologyExamSubject;
    private ExamSubject computerScienceExamSubject;
    private ExamSubjectGroup mandatoryGroup;
    private ExamSubjectGroup optionalGroup;

    @BeforeEach
    void setUp() {
        service = new ExamSubjectGroupService(
            examRepository,
            subjectRepository,
            userExamRepository,
            examSubjectRepository,
            examSubjectGroupRepository,
            examSubjectGroupItemRepository,
            userExamSubjectSelectionRepository,
            new UserMapper()
        );

        exam = Exam.builder().id(1L).name("UPSC").build();
        userExam = UserExam.builder().id(11L).exam(exam).isActive(true).build();

        math = subject(101L, "Mathematics");
        biology = subject(102L, "Biology");
        computerScience = subject(103L, "Computer Science");

        mathExamSubject = examSubject(201L, exam, math, 1);
        biologyExamSubject = examSubject(202L, exam, biology, 2);
        computerScienceExamSubject = examSubject(203L, exam, computerScience, 3);

        mandatoryGroup = group(301L, exam, "Mandatory Subjects", false, 0, 0, 1);
        optionalGroup = group(302L, exam, "Optional Paper", true, 1, 1, 2);

        mandatoryGroup.setItems(List.of(item(mandatoryGroup, math)));
        optionalGroup.setItems(List.of(item(optionalGroup, biology), item(optionalGroup, computerScience)));

        when(examSubjectGroupRepository.findByExamIdOrderByDisplayOrderAscIdAsc(exam.getId()))
            .thenReturn(List.of(mandatoryGroup, optionalGroup));
    }

    @Test
    void resolveVisibleSubjectsReturnsMandatoryAndSelectedOptionalSubjects() {
        when(examSubjectRepository.findByExamIdAndIsActiveTrueOrderByDisplayOrderAsc(exam.getId()))
            .thenReturn(List.of(mathExamSubject, biologyExamSubject, computerScienceExamSubject));
        when(userExamSubjectSelectionRepository.findByUserExamId(userExam.getId()))
            .thenReturn(List.of(selection(userExam, optionalGroup, computerScience)));

        List<ExamSubjectGroupService.ResolvedExamSubject> resolved = service.resolveVisibleSubjects(userExam);

        assertThat(resolved.stream()
            .map(resolvedSubject -> resolvedSubject.examSubject().getSubject().getName())
            .toList())
            .containsExactly("Mathematics", "Computer Science");
    }

    @Test
    void saveSelectionsRejectsMissingRequiredOptionalSelection() {
        when(userExamSubjectSelectionRepository.findByUserExamId(userExam.getId())).thenReturn(List.of());

        SubjectGroupSelectionRequest request = new SubjectGroupSelectionRequest();
        request.setGroupId(optionalGroup.getId());
        request.setSubjectIds(List.of());

        assertThatThrownBy(() -> service.saveSelections(userExam, List.of(request)))
            .isInstanceOf(BadRequestException.class)
            .hasMessageContaining("required optional subject selections");
    }

    @Test
    void saveSelectionsReplacesExistingOptionalSelection() {
        when(userExamSubjectSelectionRepository.findByUserExamId(userExam.getId()))
            .thenReturn(List.of(selection(userExam, optionalGroup, biology)));
        when(subjectRepository.findById(computerScience.getId()))
            .thenReturn(Optional.of(computerScience));

        SubjectGroupSelectionRequest request = new SubjectGroupSelectionRequest();
        request.setGroupId(optionalGroup.getId());
        request.setSubjectIds(List.of(computerScience.getId()));

        service.saveSelections(userExam, List.of(request));

        verify(userExamSubjectSelectionRepository)
            .deleteByUserExamIdAndGroupId(userExam.getId(), optionalGroup.getId());

        ArgumentCaptor<UserExamSubjectSelection> captor = ArgumentCaptor.forClass(UserExamSubjectSelection.class);
        verify(userExamSubjectSelectionRepository).save(captor.capture());
        assertThat(captor.getValue().getSubject().getId()).isEqualTo(computerScience.getId());
        assertThat(captor.getValue().getGroup().getId()).isEqualTo(optionalGroup.getId());
        assertThat(captor.getValue().getUserExam().getId()).isEqualTo(userExam.getId());
    }

    @Test
    void getGroupsByUserExamReturnsSelectedFlagsForPersistedSelections() {
        when(userExamRepository.findById(userExam.getId())).thenReturn(Optional.of(userExam));
        when(examSubjectRepository.findByExamIdAndIsActiveTrueOrderByDisplayOrderAsc(exam.getId()))
            .thenReturn(List.of(mathExamSubject, biologyExamSubject, computerScienceExamSubject));
        when(userExamSubjectSelectionRepository.findByUserExamId(userExam.getId()))
            .thenReturn(List.of(selection(userExam, optionalGroup, biology)));

        List<ExamSubjectGroupResponse> groups = service.getGroupsByUserExam(userExam.getId());

        ExamSubjectGroupResponse optional = groups.stream()
            .filter(group -> group.getId().equals(optionalGroup.getId()))
            .findFirst()
            .orElseThrow();

        assertThat(optional.getSelectedCount()).isEqualTo(1);
        assertThat(optional.getSubjects())
            .extracting(SubjectResponse::getId, SubjectResponse::getSelected)
            .containsExactly(
                org.assertj.core.groups.Tuple.tuple(biology.getId(), true),
                org.assertj.core.groups.Tuple.tuple(computerScience.getId(), false)
            );
    }

    private Subject subject(Long id, String name) {
        return Subject.builder()
            .id(id)
            .name(name)
            .normalizedName(name.toLowerCase())
            .iconName("menu_book")
            .colorCode("#1565C0")
            .build();
    }

    private ExamSubject examSubject(Long id, Exam exam, Subject subject, int displayOrder) {
        return ExamSubject.builder()
            .id(id)
            .exam(exam)
            .subject(subject)
            .displayOrder(displayOrder)
            .isActive(true)
            .build();
    }

    private ExamSubjectGroup group(Long id, Exam exam, String name, boolean isOptional, int minSelection, int maxSelection, int displayOrder) {
        return ExamSubjectGroup.builder()
            .id(id)
            .exam(exam)
            .groupName(name)
            .isOptional(isOptional)
            .minSelection(minSelection)
            .maxSelection(maxSelection)
            .displayOrder(displayOrder)
            .build();
    }

    private ExamSubjectGroupItem item(ExamSubjectGroup group, Subject subject) {
        return ExamSubjectGroupItem.builder()
            .group(group)
            .subject(subject)
            .build();
    }

    private UserExamSubjectSelection selection(UserExam userExam, ExamSubjectGroup group, Subject subject) {
        return UserExamSubjectSelection.builder()
            .userExam(userExam)
            .group(group)
            .subject(subject)
            .build();
    }
}
package com.examsaathi.service;

import com.examsaathi.dto.request.SubjectRequest;
import com.examsaathi.dto.response.SubjectResponse;
import com.examsaathi.entity.Exam;
import com.examsaathi.entity.Subject;
import com.examsaathi.exception.ResourceNotFoundException;
import com.examsaathi.repository.ExamRepository;
import com.examsaathi.repository.SubjectRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class SubjectService {

    private final SubjectRepository subjectRepository;
    private final ExamRepository examRepository;
    private final UserMapper mapper;

    @Transactional(readOnly = true)
    public List<SubjectResponse> getSubjectsByExam(Long examId) {
        return subjectRepository.findByExamIdAndIsActiveTrueOrderByDisplayOrderAsc(examId)
            .stream().map(s -> mapper.toSubjectResponse(s, false)).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public SubjectResponse getSubjectById(Long id) {
        Subject subject = subjectRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Subject", id));
        return mapper.toSubjectResponse(subject, true);
    }

    @Transactional
    public SubjectResponse createSubject(SubjectRequest request) {
        Exam exam = examRepository.findById(request.getExamId())
            .orElseThrow(() -> new ResourceNotFoundException("Exam", request.getExamId()));

        Subject subject = Subject.builder()
            .exam(exam)
            .name(request.getName())
            .description(request.getDescription())
            .iconName(request.getIconName())
            .colorCode(request.getColorCode())
            .displayOrder(request.getDisplayOrder())
            .isActive(request.getIsActive())
            .build();

        return mapper.toSubjectResponse(subjectRepository.save(subject), false);
    }

    @Transactional
    public SubjectResponse updateSubject(Long id, SubjectRequest request) {
        Subject subject = subjectRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Subject", id));

        subject.setName(request.getName());
        subject.setDescription(request.getDescription());
        subject.setIconName(request.getIconName());
        subject.setColorCode(request.getColorCode());
        subject.setDisplayOrder(request.getDisplayOrder());
        if (request.getIsActive() != null) subject.setIsActive(request.getIsActive());

        return mapper.toSubjectResponse(subjectRepository.save(subject), false);
    }

    @Transactional
    public void deleteSubject(Long id) {
        if (!subjectRepository.existsById(id)) {
            throw new ResourceNotFoundException("Subject", id);
        }
        subjectRepository.deleteById(id);
    }
}

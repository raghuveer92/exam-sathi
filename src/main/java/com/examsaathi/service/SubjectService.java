package com.examsaathi.service;

import com.examsaathi.config.CacheNames;
import com.examsaathi.dto.request.SubjectRequest;
import com.examsaathi.dto.request.CloneSubjectRequest;
import com.examsaathi.dto.response.SubjectResponse;
import com.examsaathi.entity.Exam;
import com.examsaathi.entity.ExamSubject;
import com.examsaathi.entity.Subject;
import com.examsaathi.exception.ResourceNotFoundException;
import com.examsaathi.repository.ExamRepository;
import com.examsaathi.repository.ExamSubjectRepository;
import com.examsaathi.repository.SubjectRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class SubjectService {

    private final SubjectRepository subjectRepository;
    private final ExamRepository examRepository;
    private final ExamSubjectRepository examSubjectRepository;
    private final ExamSubjectGroupService examSubjectGroupService;
    private final UserMapper mapper;
    private final CacheEvictionService cacheEvictionService;

    @Transactional(readOnly = true)
    @Cacheable(value = CacheNames.SUBJECTS_BY_EXAM, key = "'exam_' + #examId")
    public List<SubjectResponse> getSubjectsByExam(Long examId) {
        return examSubjectGroupService.getAllGroupedSubjects(examId).stream()
            .map(resolved -> examSubjectGroupService.toSubjectResponse(resolved, false))
            .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    @Cacheable(value = CacheNames.SUBJECTS, key = "'subject_' + #id")
    public SubjectResponse getSubjectById(Long id) {
        Subject subject = subjectRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Subject", id));
        return mapper.toSubjectResponse(subject, true);
    }

    @Transactional
    public SubjectResponse createSubject(SubjectRequest request) {
        Exam exam = examRepository.findById(request.getExamId())
            .orElseThrow(() -> new ResourceNotFoundException("Exam", request.getExamId()));

        Subject subject = subjectRepository.findByNormalizedName(normalize(request.getName()))
            .map(existing -> {
                existing.setDescription(request.getDescription());
                existing.setIconName(request.getIconName());
                existing.setColorCode(request.getColorCode());
                if (request.getIsActive() != null) {
                    existing.setIsActive(request.getIsActive());
                }
                return existing;
            })
            .orElseGet(() -> Subject.builder()
                .name(request.getName())
                .normalizedName(normalize(request.getName()))
                .description(request.getDescription())
                .iconName(request.getIconName())
                .colorCode(request.getColorCode())
                .isActive(request.getIsActive())
                .build());

        Subject savedSubject = subjectRepository.save(subject);
        ExamSubject examSubject = examSubjectRepository.findByExamIdAndSubjectId(exam.getId(), savedSubject.getId())
            .orElseGet(() -> ExamSubject.builder()
                .exam(exam)
                .subject(savedSubject)
                .build());
        examSubject.setDisplayOrder(request.getDisplayOrder() != null ? request.getDisplayOrder() : 0);
        if (request.getIsActive() != null) {
            examSubject.setIsActive(request.getIsActive());
        }

        ExamSubject savedExamSubject = examSubjectRepository.save(examSubject);
        examSubjectGroupService.assignSubjectToGroup(exam.getId(), savedSubject.getId(), request.getGroupId());

        SubjectResponse response = examSubjectGroupService.getAllGroupedSubjects(exam.getId()).stream()
            .filter(resolved -> resolved.examSubject().getId().equals(savedExamSubject.getId()))
            .findFirst()
            .map(resolved -> examSubjectGroupService.toSubjectResponse(resolved, false))
            .orElseGet(() -> mapper.toSubjectResponse(savedExamSubject, false));
        cacheEvictionService.evictCatalogData();
        return response;
    }

    @Transactional
    public SubjectResponse updateSubject(Long id, SubjectRequest request) {
        Subject subject = subjectRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Subject", id));

        subject.setName(request.getName());
        subject.setNormalizedName(normalize(request.getName()));
        subject.setDescription(request.getDescription());
        subject.setIconName(request.getIconName());
        subject.setColorCode(request.getColorCode());
        if (request.getIsActive() != null) subject.setIsActive(request.getIsActive());

        ExamSubject examSubject = examSubjectRepository.findByExamIdAndSubjectId(request.getExamId(), id)
            .orElseGet(() -> ExamSubject.builder()
                .exam(examRepository.findById(request.getExamId())
                    .orElseThrow(() -> new ResourceNotFoundException("Exam", request.getExamId())))
                .subject(subject)
                .build());
        examSubject.setDisplayOrder(request.getDisplayOrder() != null ? request.getDisplayOrder() : 0);
        if (request.getIsActive() != null) {
            examSubject.setIsActive(request.getIsActive());
        }

        subjectRepository.save(subject);
        ExamSubject savedExamSubject = examSubjectRepository.save(examSubject);
        examSubjectGroupService.assignSubjectToGroup(request.getExamId(), subject.getId(), request.getGroupId());
        SubjectResponse response = examSubjectGroupService.getAllGroupedSubjects(request.getExamId()).stream()
            .filter(resolved -> resolved.examSubject().getId().equals(savedExamSubject.getId()))
            .findFirst()
            .map(resolved -> examSubjectGroupService.toSubjectResponse(resolved, false))
            .orElseGet(() -> mapper.toSubjectResponse(savedExamSubject, false));
        cacheEvictionService.evictCatalogData();
        return response;
    }

    @Transactional
    public void deleteSubject(Long id, Long examId) {
        Subject subject = subjectRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Subject", id));

        ExamSubject examSubject = examSubjectRepository.findByExamIdAndSubjectId(examId, id)
            .orElseThrow(() -> new ResourceNotFoundException("Subject for exam", id));

        examSubjectGroupService.removeSubjectAssignments(examId, id);
        examSubjectRepository.delete(examSubject);

        if (examSubjectRepository.countBySubjectId(subject.getId()) == 0) {
            subjectRepository.delete(subject);
        }
        cacheEvictionService.evictCatalogData();
    }

    @Transactional
    public SubjectResponse cloneSubject(Long sourceSubjectId, CloneSubjectRequest request) {
        Subject sourceSubject = subjectRepository.findById(sourceSubjectId)
            .orElseThrow(() -> new ResourceNotFoundException("Subject", sourceSubjectId));
        Exam targetExam = examRepository.findById(request.getTargetExamId())
            .orElseThrow(() -> new ResourceNotFoundException("Exam", request.getTargetExamId()));

        ExamSubject examSubject = examSubjectRepository.findByExamIdAndSubjectId(targetExam.getId(), sourceSubject.getId())
            .orElseGet(() -> ExamSubject.builder()
                .exam(targetExam)
                .subject(sourceSubject)
                .build());
        examSubject.setDisplayOrder(request.getDisplayOrder() != null ? request.getDisplayOrder() : 0);
        examSubject.setIsActive(request.getIsActive() != null ? request.getIsActive() : sourceSubject.getIsActive());

        ExamSubject savedExamSubject = examSubjectRepository.save(examSubject);
        examSubjectGroupService.assignSubjectToGroup(targetExam.getId(), sourceSubject.getId(), request.getGroupId());
        SubjectResponse response = examSubjectGroupService.getAllGroupedSubjects(targetExam.getId()).stream()
            .filter(resolved -> resolved.examSubject().getId().equals(savedExamSubject.getId()))
            .findFirst()
            .map(resolved -> examSubjectGroupService.toSubjectResponse(resolved, false))
            .orElseGet(() -> mapper.toSubjectResponse(savedExamSubject, false));
        cacheEvictionService.evictCatalogData();
        return response;
    }

    private String normalize(String value) {
        return value == null ? null : value.trim().replaceAll("\\s+", " ").toLowerCase(Locale.ROOT);
    }
}

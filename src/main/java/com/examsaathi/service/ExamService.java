package com.examsaathi.service;

import com.examsaathi.dto.request.ExamRequest;
import com.examsaathi.dto.response.ExamResponse;
import com.examsaathi.entity.Exam;
import com.examsaathi.exception.ResourceNotFoundException;
import com.examsaathi.repository.ExamRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ExamService {

    private final ExamRepository examRepository;
    private final UserMapper mapper;

    @Transactional(readOnly = true)
    public List<ExamResponse> getAllActiveExams() {
        return examRepository.findByIsActiveTrueOrderByNameAsc()
            .stream().map(e -> mapper.toExamResponse(e, false)).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public ExamResponse getExamById(Long id) {
        Exam exam = examRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Exam", id));
        return mapper.toExamResponse(exam, true);
    }

    @Transactional
    public ExamResponse createExam(ExamRequest request) {
        Exam exam = Exam.builder()
            .name(request.getName())
            .description(request.getDescription())
            .code(request.getCode())
            .iconUrl(request.getIconUrl())
            .colorCode(request.getColorCode())
            .isActive(request.getIsActive())
            .build();
        return mapper.toExamResponse(examRepository.save(exam), false);
    }

    @Transactional
    public ExamResponse updateExam(Long id, ExamRequest request) {
        Exam exam = examRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Exam", id));
        exam.setName(request.getName());
        exam.setDescription(request.getDescription());
        exam.setCode(request.getCode());
        exam.setIconUrl(request.getIconUrl());
        exam.setColorCode(request.getColorCode());
        if (request.getIsActive() != null) exam.setIsActive(request.getIsActive());
        return mapper.toExamResponse(examRepository.save(exam), false);
    }

    @Transactional
    public void deleteExam(Long id) {
        if (!examRepository.existsById(id)) {
            throw new ResourceNotFoundException("Exam", id);
        }
        examRepository.deleteById(id);
    }
}

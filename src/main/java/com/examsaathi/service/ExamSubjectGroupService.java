package com.examsaathi.service;

import com.examsaathi.config.CacheNames;
import com.examsaathi.dto.request.ExamSubjectGroupRequest;
import com.examsaathi.dto.request.SubjectGroupSelectionRequest;
import com.examsaathi.dto.response.ExamSubjectGroupResponse;
import com.examsaathi.dto.response.SubjectResponse;
import com.examsaathi.entity.*;
import com.examsaathi.exception.BadRequestException;
import com.examsaathi.exception.ResourceNotFoundException;
import com.examsaathi.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ExamSubjectGroupService {

    private static final String DEFAULT_MANDATORY_GROUP = "Mandatory Subjects";

    private final ExamRepository examRepository;
    private final SubjectRepository subjectRepository;
    private final UserExamRepository userExamRepository;
    private final ExamSubjectRepository examSubjectRepository;
    private final ExamSubjectGroupRepository examSubjectGroupRepository;
    private final ExamSubjectGroupItemRepository examSubjectGroupItemRepository;
    private final UserExamSubjectSelectionRepository userExamSubjectSelectionRepository;
    private final UserMapper userMapper;
    private final CacheEvictionService cacheEvictionService;

    @Transactional
    public ExamSubjectGroup ensureDefaultMandatoryGroup(Long examId) {
        Exam exam = examRepository.findById(examId)
            .orElseThrow(() -> new ResourceNotFoundException("Exam", examId));
        return ensureDefaultMandatoryGroup(exam);
    }

    @Transactional
    public ExamSubjectGroup ensureDefaultMandatoryGroup(Exam exam) {
        ExamSubjectGroup group = examSubjectGroupRepository.findByExamIdAndGroupNameIgnoreCase(exam.getId(), DEFAULT_MANDATORY_GROUP)
            .orElseGet(() -> examSubjectGroupRepository.save(ExamSubjectGroup.builder()
                .exam(exam)
                .groupName(DEFAULT_MANDATORY_GROUP)
                .isOptional(false)
                .minSelection(0)
                .maxSelection(0)
                .displayOrder(0)
                .build()));

        seedUngroupedSubjects(exam.getId(), group);
        return group;
    }

    @Transactional(readOnly = true)
    @Cacheable(value = CacheNames.EXAM_SUBJECT_GROUPS, key = "'exam_' + #examId")
    public List<ExamSubjectGroupResponse> getGroupsByExam(Long examId) {
        return buildGroupResponses(examId, null);
    }

    @Transactional(readOnly = true)
    public List<ExamSubjectGroupResponse> getGroupsByUserExam(Long userExamId) {
        UserExam userExam = userExamRepository.findById(userExamId)
            .orElseThrow(() -> new ResourceNotFoundException("User exam", userExamId));
        return buildGroupResponses(userExam.getExam().getId(), userExam.getId());
    }

    @Transactional
    public ExamSubjectGroupResponse createGroup(ExamSubjectGroupRequest request) {
        validateSelectionBounds(request.getIsOptional(), request.getMinSelection(), request.getMaxSelection());
        Exam exam = examRepository.findById(request.getExamId())
            .orElseThrow(() -> new ResourceNotFoundException("Exam", request.getExamId()));

        ExamSubjectGroup group = examSubjectGroupRepository.save(ExamSubjectGroup.builder()
            .exam(exam)
            .groupName(request.getGroupName().trim())
            .isOptional(Boolean.TRUE.equals(request.getIsOptional()))
            .minSelection(Boolean.TRUE.equals(request.getIsOptional()) ? sanitizeCount(request.getMinSelection()) : 0)
            .maxSelection(Boolean.TRUE.equals(request.getIsOptional()) ? sanitizeCount(request.getMaxSelection()) : 0)
            .displayOrder(request.getDisplayOrder() != null ? request.getDisplayOrder() : 0)
            .build());

        addSubjectsToGroup(group, request.getSubjectIds());
        ExamSubjectGroupResponse response = buildGroupResponses(exam.getId(), null).stream()
            .filter(r -> r.getId().equals(group.getId()))
            .findFirst()
            .orElseThrow();
        cacheEvictionService.evictCatalogData();
        return response;
    }

    @Transactional
    public ExamSubjectGroupResponse updateGroup(Long groupId, ExamSubjectGroupRequest request) {
        validateSelectionBounds(request.getIsOptional(), request.getMinSelection(), request.getMaxSelection());
        ExamSubjectGroup group = examSubjectGroupRepository.findById(groupId)
            .orElseThrow(() -> new ResourceNotFoundException("Exam subject group", groupId));

        if (!group.getExam().getId().equals(request.getExamId())) {
            throw new BadRequestException("Group cannot be moved to a different exam");
        }

        group.setGroupName(request.getGroupName().trim());
        group.setIsOptional(Boolean.TRUE.equals(request.getIsOptional()));
        group.setMinSelection(Boolean.TRUE.equals(group.getIsOptional()) ? sanitizeCount(request.getMinSelection()) : 0);
        group.setMaxSelection(Boolean.TRUE.equals(group.getIsOptional()) ? sanitizeCount(request.getMaxSelection()) : 0);
        group.setDisplayOrder(request.getDisplayOrder() != null ? request.getDisplayOrder() : 0);
        examSubjectGroupRepository.save(group);

        addSubjectsToGroup(group, request.getSubjectIds());
        ExamSubjectGroupResponse response = buildGroupResponses(group.getExam().getId(), null).stream()
            .filter(r -> r.getId().equals(groupId))
            .findFirst()
            .orElseThrow();
        cacheEvictionService.evictCatalogData();
        return response;
    }

    @Transactional
    public void deleteGroup(Long groupId) {
        ExamSubjectGroup group = examSubjectGroupRepository.findById(groupId)
            .orElseThrow(() -> new ResourceNotFoundException("Exam subject group", groupId));

        if (!group.getItems().isEmpty()) {
            throw new BadRequestException("Remove or reassign subjects before deleting this group");
        }

        examSubjectGroupRepository.delete(group);
        cacheEvictionService.evictCatalogData();
    }

    @Transactional
    public void assignSubjectToGroup(Long examId, Long subjectId, Long groupId) {
        Subject subject = subjectRepository.findById(subjectId)
            .orElseThrow(() -> new ResourceNotFoundException("Subject", subjectId));

        examSubjectRepository.findByExamIdAndSubjectId(examId, subjectId)
            .orElseThrow(() -> new BadRequestException("Subject is not linked to the target exam"));

        ExamSubjectGroup targetGroup = resolveTargetGroup(examId, groupId);
        Optional<ExamSubjectGroupItem> existingAssignment = examSubjectGroupItemRepository
            .findByGroupExamIdAndSubjectId(examId, subjectId);

        if (existingAssignment.isPresent()) {
            ExamSubjectGroupItem existingItem = existingAssignment.get();
            if (existingItem.getGroup().getId().equals(targetGroup.getId())) {
                return;
            }

            examSubjectGroupItemRepository.delete(existingItem);
        }

        examSubjectGroupItemRepository.save(ExamSubjectGroupItem.builder()
            .group(targetGroup)
            .subject(subject)
            .build());
    }

    @Transactional
    public void removeSubjectAssignments(Long examId, Long subjectId) {
        examSubjectGroupItemRepository.deleteByGroupExamIdAndSubjectId(examId, subjectId);
        userExamSubjectSelectionRepository.deleteByGroupExamIdAndSubjectId(examId, subjectId);
    }

    @Transactional(readOnly = true)
    public boolean hasRequiredOptionalGroups(Long examId) {
        return loadGroups(examId).stream()
            .anyMatch(group -> Boolean.TRUE.equals(group.getIsOptional()) && sanitizeCount(group.getMinSelection()) > 0);
    }

    @Transactional(readOnly = true)
    public boolean hasValidSelections(UserExam userExam) {
        try {
            validateSelections(userExam, Collections.emptyList(), false);
            return true;
        } catch (BadRequestException ex) {
            return false;
        }
    }

    @Transactional
    public void saveSelections(UserExam userExam, List<SubjectGroupSelectionRequest> requests) {
        Map<Long, List<Long>> requestedSelections = validateSelections(userExam, requests, true);
        List<ExamSubjectGroup> optionalGroups = loadGroups(userExam.getExam().getId()).stream()
            .filter(group -> Boolean.TRUE.equals(group.getIsOptional()))
            .toList();

        for (ExamSubjectGroup group : optionalGroups) {
            userExamSubjectSelectionRepository.deleteByUserExamIdAndGroupId(userExam.getId(), group.getId());
            for (Long subjectId : requestedSelections.getOrDefault(group.getId(), Collections.emptyList())) {
                Subject subject = subjectRepository.findById(subjectId)
                    .orElseThrow(() -> new ResourceNotFoundException("Subject", subjectId));
                userExamSubjectSelectionRepository.save(UserExamSubjectSelection.builder()
                    .userExam(userExam)
                    .group(group)
                    .subject(subject)
                    .build());
            }
        }
    }

    @Transactional(readOnly = true)
    public List<ResolvedExamSubject> resolveVisibleSubjects(UserExam userExam) {
        List<ExamSubject> activeExamSubjects = examSubjectRepository.findByExamIdAndIsActiveTrueOrderByDisplayOrderAsc(userExam.getExam().getId());
        if (activeExamSubjects.isEmpty()) {
            return Collections.emptyList();
        }

        List<ExamSubjectGroup> groups = loadGroups(userExam.getExam().getId());
        if (groups.isEmpty()) {
            return activeExamSubjects.stream()
                .map(examSubject -> new ResolvedExamSubject(examSubject, null, true))
                .toList();
        }

        Map<Long, ExamSubject> examSubjectsBySubjectId = activeExamSubjects.stream()
            .collect(Collectors.toMap(es -> es.getSubject().getId(), Function.identity(), (left, right) -> left, LinkedHashMap::new));

        Map<Long, Set<Long>> selectionsByGroup = userExamSubjectSelectionRepository.findByUserExamId(userExam.getId()).stream()
            .collect(Collectors.groupingBy(selection -> selection.getGroup().getId(),
                Collectors.mapping(selection -> selection.getSubject().getId(), Collectors.toCollection(LinkedHashSet::new))));

        List<ResolvedExamSubject> resolved = new ArrayList<>();
        for (ExamSubjectGroup group : groups) {
            Set<Long> groupSubjectIds = group.getItems().stream()
                .map(item -> item.getSubject().getId())
                .filter(examSubjectsBySubjectId::containsKey)
                .collect(Collectors.toCollection(LinkedHashSet::new));

            Set<Long> visibleIds = Boolean.TRUE.equals(group.getIsOptional())
                ? selectionsByGroup.getOrDefault(group.getId(), Collections.emptySet())
                : groupSubjectIds;

            for (Long subjectId : visibleIds) {
                if (!groupSubjectIds.contains(subjectId)) {
                    continue;
                }
                ExamSubject examSubject = examSubjectsBySubjectId.get(subjectId);
                if (examSubject != null) {
                    resolved.add(new ResolvedExamSubject(examSubject, group, true));
                }
            }
        }

        return resolved.stream()
            .sorted(Comparator
                .comparing((ResolvedExamSubject resolvedSubject) -> resolvedSubject.group() != null ? resolvedSubject.group().getDisplayOrder() : 0)
                .thenComparing(resolvedSubject -> resolvedSubject.examSubject().getDisplayOrder()))
            .toList();
    }

    @Transactional(readOnly = true)
    public List<ResolvedExamSubject> getAllGroupedSubjects(Long examId) {
        List<ExamSubject> activeExamSubjects = examSubjectRepository.findByExamIdAndIsActiveTrueOrderByDisplayOrderAsc(examId);
        if (activeExamSubjects.isEmpty()) {
            return Collections.emptyList();
        }

        List<ExamSubjectGroup> groups = loadGroups(examId);
        if (groups.isEmpty()) {
            return activeExamSubjects.stream()
                .map(examSubject -> new ResolvedExamSubject(examSubject, null, true))
                .toList();
        }

        Map<Long, ExamSubject> examSubjectsBySubjectId = activeExamSubjects.stream()
            .collect(Collectors.toMap(es -> es.getSubject().getId(), Function.identity()));

        List<ResolvedExamSubject> resolved = new ArrayList<>();
        for (ExamSubjectGroup group : groups) {
            for (ExamSubjectGroupItem item : group.getItems()) {
                ExamSubject examSubject = examSubjectsBySubjectId.get(item.getSubject().getId());
                if (examSubject != null) {
                    resolved.add(new ResolvedExamSubject(examSubject, group, !Boolean.TRUE.equals(group.getIsOptional())));
                }
            }
        }

        if (resolved.isEmpty()) {
            return activeExamSubjects.stream()
                .map(examSubject -> new ResolvedExamSubject(examSubject, null, true))
                .toList();
        }

        return resolved.stream()
            .sorted(Comparator
                .comparing((ResolvedExamSubject resolvedSubject) -> resolvedSubject.group() != null ? resolvedSubject.group().getDisplayOrder() : 0)
                .thenComparing(resolvedSubject -> resolvedSubject.examSubject().getDisplayOrder()))
            .toList();
    }

    @Transactional(readOnly = true)
    public List<Long> getVisibleSubjectIds(UserExam userExam) {
        return resolveVisibleSubjects(userExam).stream()
            .map(resolved -> resolved.examSubject().getSubject().getId())
            .toList();
    }

    @Transactional(readOnly = true)
    public int countVisibleSubjects(UserExam userExam) {
        return resolveVisibleSubjects(userExam).size();
    }

    @Transactional(readOnly = true)
    public boolean isSubjectVisible(UserExam userExam, Long subjectId) {
        return resolveVisibleSubjects(userExam).stream()
            .anyMatch(resolved -> resolved.examSubject().getSubject().getId().equals(subjectId));
    }

    @Transactional(readOnly = true)
    public SubjectResponse toSubjectResponse(ResolvedExamSubject resolvedSubject, boolean includeChapters) {
        return userMapper.toSubjectResponse(
            resolvedSubject.examSubject(),
            resolvedSubject.group(),
            includeChapters,
            resolvedSubject.selected()
        );
    }

    private List<ExamSubjectGroupResponse> buildGroupResponses(Long examId, Long userExamId) {
        List<ExamSubjectGroup> groups = loadGroups(examId);
        if (groups.isEmpty()) {
            return Collections.emptyList();
        }

        Map<Long, Set<Long>> selectionsByGroup = userExamId == null
            ? Collections.emptyMap()
            : userExamSubjectSelectionRepository.findByUserExamId(userExamId).stream()
                .collect(Collectors.groupingBy(selection -> selection.getGroup().getId(),
                    Collectors.mapping(selection -> selection.getSubject().getId(), Collectors.toCollection(LinkedHashSet::new))));

        Map<Long, ExamSubject> examSubjectsBySubjectId = examSubjectRepository.findByExamIdAndIsActiveTrueOrderByDisplayOrderAsc(examId).stream()
            .collect(Collectors.toMap(es -> es.getSubject().getId(), Function.identity()));

        return groups.stream()
            .map(group -> {
                Set<Long> selectedIds = selectionsByGroup.getOrDefault(group.getId(), Collections.emptySet());
                List<SubjectResponse> subjects = group.getItems().stream()
                    .map(ExamSubjectGroupItem::getSubject)
                    .map(subject -> {
                        ExamSubject examSubject = examSubjectsBySubjectId.get(subject.getId());
                        return examSubject == null
                            ? null
                            : userMapper.toSubjectResponse(examSubject, group, false, selectedIds.contains(subject.getId()));
                    })
                    .filter(Objects::nonNull)
                    .toList();

                return ExamSubjectGroupResponse.builder()
                    .id(group.getId())
                    .examId(group.getExam().getId())
                    .groupName(group.getGroupName())
                    .isOptional(group.getIsOptional())
                    .minSelection(group.getMinSelection())
                    .maxSelection(group.getMaxSelection())
                    .displayOrder(group.getDisplayOrder())
                    .selectedCount(selectedIds.size())
                    .createdAt(group.getCreatedAt())
                    .subjects(subjects)
                    .build();
            })
            .toList();
    }

    private void addSubjectsToGroup(ExamSubjectGroup group, List<Long> subjectIds) {
        if (subjectIds == null || subjectIds.isEmpty()) {
            return;
        }
        for (Long subjectId : new LinkedHashSet<>(subjectIds)) {
            assignSubjectToGroup(group.getExam().getId(), subjectId, group.getId());
        }
    }

    private ExamSubjectGroup resolveTargetGroup(Long examId, Long groupId) {
        if (groupId == null) {
            return ensureDefaultMandatoryGroup(examId);
        }
        ExamSubjectGroup group = examSubjectGroupRepository.findById(groupId)
            .orElseThrow(() -> new ResourceNotFoundException("Exam subject group", groupId));
        if (!group.getExam().getId().equals(examId)) {
            throw new BadRequestException("Group does not belong to the target exam");
        }
        return group;
    }

    private void seedUngroupedSubjects(Long examId, ExamSubjectGroup defaultGroup) {
        List<ExamSubject> examSubjects = examSubjectRepository.findByExamIdAndIsActiveTrueOrderByDisplayOrderAsc(examId);
        for (ExamSubject examSubject : examSubjects) {
            boolean assigned = examSubjectGroupItemRepository.existsByGroupExamIdAndSubjectId(examId, examSubject.getSubject().getId());
            if (!assigned) {
                examSubjectGroupItemRepository.save(ExamSubjectGroupItem.builder()
                    .group(defaultGroup)
                    .subject(examSubject.getSubject())
                    .build());
            }
        }
    }

    private List<ExamSubjectGroup> loadGroups(Long examId) {
        return examSubjectGroupRepository.findByExamIdOrderByDisplayOrderAscIdAsc(examId);
    }

    private Map<Long, List<Long>> validateSelections(UserExam userExam, List<SubjectGroupSelectionRequest> requests, boolean strictPayload) {
        List<ExamSubjectGroup> groups = loadGroups(userExam.getExam().getId());
        List<ExamSubjectGroup> optionalGroups = groups.stream()
            .filter(group -> Boolean.TRUE.equals(group.getIsOptional()))
            .toList();

        if (optionalGroups.isEmpty()) {
            return Collections.emptyMap();
        }

        Map<Long, SubjectGroupSelectionRequest> requestsByGroup = (requests == null ? Collections.<SubjectGroupSelectionRequest>emptyList() : requests)
            .stream()
            .collect(Collectors.toMap(SubjectGroupSelectionRequest::getGroupId, Function.identity(), (left, right) -> right, LinkedHashMap::new));

        if (strictPayload) {
            for (Long groupId : requestsByGroup.keySet()) {
                boolean belongsToExam = optionalGroups.stream().anyMatch(group -> group.getId().equals(groupId));
                if (!belongsToExam) {
                    throw new BadRequestException("Invalid optional subject group selection");
                }
            }
        }

        Map<Long, List<Long>> normalized = new LinkedHashMap<>();
        Map<Long, Set<Long>> persistedSelections = userExamSubjectSelectionRepository.findByUserExamId(userExam.getId()).stream()
            .collect(Collectors.groupingBy(selection -> selection.getGroup().getId(),
                Collectors.mapping(selection -> selection.getSubject().getId(), Collectors.toCollection(LinkedHashSet::new))));

        for (ExamSubjectGroup group : optionalGroups) {
            List<Long> subjectIds = requestsByGroup.containsKey(group.getId())
                ? dedupe(requestsByGroup.get(group.getId()).getSubjectIds())
                : (strictPayload ? Collections.emptyList() : new ArrayList<>(persistedSelections.getOrDefault(group.getId(), Collections.emptySet())));

            Set<Long> allowedSubjectIds = group.getItems().stream()
                .map(item -> item.getSubject().getId())
                .collect(Collectors.toCollection(LinkedHashSet::new));

            if (!allowedSubjectIds.containsAll(subjectIds)) {
                throw new BadRequestException("One or more selected subjects do not belong to the requested group");
            }

            int selectionCount = subjectIds.size();
            int min = sanitizeCount(group.getMinSelection());
            int max = sanitizeCount(group.getMaxSelection());
            if (selectionCount < min) {
                throw new BadRequestException("Please complete all required optional subject selections");
            }
            if (max > 0 && selectionCount > max) {
                throw new BadRequestException("Too many optional subjects selected for group " + group.getGroupName());
            }

            normalized.put(group.getId(), subjectIds);
        }

        return normalized;
    }

    private void validateSelectionBounds(Boolean isOptional, Integer minSelection, Integer maxSelection) {
        int min = sanitizeCount(minSelection);
        int max = sanitizeCount(maxSelection);
        if (!Boolean.TRUE.equals(isOptional)) {
            return;
        }
        if (max < min) {
            throw new BadRequestException("Max selection cannot be less than min selection");
        }
    }

    private int sanitizeCount(Integer value) {
        return value == null ? 0 : Math.max(value, 0);
    }

    private List<Long> dedupe(List<Long> values) {
        if (values == null || values.isEmpty()) {
            return Collections.emptyList();
        }
        return new ArrayList<>(new LinkedHashSet<>(values));
    }

    public record ResolvedExamSubject(ExamSubject examSubject, ExamSubjectGroup group, boolean selected) {
    }
}
package com.examsaathi.service;

import com.examsaathi.config.CacheNames;
import com.examsaathi.dto.request.ExamCategoryRequest;
import com.examsaathi.dto.response.ExamCategoryResponse;
import com.examsaathi.entity.ExamCategory;
import com.examsaathi.exception.BadRequestException;
import com.examsaathi.exception.ResourceNotFoundException;
import com.examsaathi.repository.ExamCategoryRepository;
import com.examsaathi.repository.ExamRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ExamCategoryService {

    private final ExamCategoryRepository categoryRepository;
    private final ExamRepository examRepository;
    private final CacheEvictionService cacheEvictionService;

    @Transactional(readOnly = true)
    @Cacheable(value = CacheNames.EXAM_CATEGORIES, key = "'active'")
    public List<ExamCategoryResponse> getActiveCategories() {
        return categoryRepository.findByIsActiveTrueOrderByDisplayOrderAscNameAsc()
            .stream().map(this::toResponse).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    @Cacheable(value = CacheNames.EXAM_CATEGORIES, key = "'all'")
    public List<ExamCategoryResponse> getAllCategories() {
        return categoryRepository.findAllByOrderByDisplayOrderAscNameAsc()
            .stream().map(this::toResponse).collect(Collectors.toList());
    }

    @Transactional
    public ExamCategoryResponse create(ExamCategoryRequest request) {
        if (categoryRepository.existsByNameIgnoreCase(request.getName())) {
            throw new BadRequestException("Category already exists: " + request.getName());
        }
        ExamCategory category = ExamCategory.builder()
            .name(request.getName().trim())
            .description(request.getDescription())
            .icon(request.getIcon())
            .displayOrder(request.getDisplayOrder() != null ? request.getDisplayOrder() : 0)
            .isActive(request.getIsActive() != null ? request.getIsActive() : true)
            .build();
        ExamCategoryResponse response = toResponse(categoryRepository.save(category));
        cacheEvictionService.evictCatalogData();
        return response;
    }

    @Transactional
    public ExamCategoryResponse update(Long id, ExamCategoryRequest request) {
        ExamCategory category = categoryRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("ExamCategory", id));
        category.setName(request.getName().trim());
        category.setDescription(request.getDescription());
        category.setIcon(request.getIcon());
        if (request.getDisplayOrder() != null) category.setDisplayOrder(request.getDisplayOrder());
        if (request.getIsActive() != null) category.setIsActive(request.getIsActive());
        ExamCategoryResponse response = toResponse(categoryRepository.save(category));
        cacheEvictionService.evictCatalogData();
        return response;
    }

    @Transactional
    public void delete(Long id) {
        ExamCategory category = categoryRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("ExamCategory", id));
        long linked = examRepository.findAll().stream()
            .filter(e -> e.getCategory() != null && e.getCategory().getId().equals(id))
            .count();
        if (linked > 0) {
            throw new BadRequestException("Cannot delete category with linked exams");
        }
        categoryRepository.delete(category);
        cacheEvictionService.evictCatalogData();
    }

    public ExamCategory getEntity(Long id) {
        return categoryRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("ExamCategory", id));
    }

    private ExamCategoryResponse toResponse(ExamCategory category) {
        int count = (int) examRepository.findByIsActiveTrueAndCategoryIdOrderByDisplayOrderAscNameAsc(category.getId()).size();
        return ExamCategoryResponse.builder()
            .id(category.getId())
            .name(category.getName())
            .description(category.getDescription())
            .icon(category.getIcon())
            .displayOrder(category.getDisplayOrder())
            .isActive(category.getIsActive())
            .examCount(count)
            .createdAt(category.getCreatedAt())
            .build();
    }
}

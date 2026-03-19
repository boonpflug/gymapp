package com.gymplatform.modules.booking;

import com.gymplatform.config.multitenancy.TenantContext;
import com.gymplatform.modules.booking.dto.ClassCategoryDto;
import com.gymplatform.modules.booking.dto.CreateClassCategoryRequest;
import com.gymplatform.shared.BusinessException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ClassCategoryService {

    private final ClassCategoryRepository categoryRepository;

    public List<ClassCategoryDto> getAllActive() {
        return categoryRepository.findByActiveTrue().stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    public List<ClassCategoryDto> getAll() {
        return categoryRepository.findAll().stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    public ClassCategoryDto getById(UUID id) {
        ClassCategory cat = categoryRepository.findById(id)
                .orElseThrow(() -> BusinessException.notFound("ClassCategory", id));
        return toDto(cat);
    }

    @Transactional
    public ClassCategoryDto create(CreateClassCategoryRequest req) {
        ClassCategory cat = ClassCategory.builder()
                .name(req.getName())
                .description(req.getDescription())
                .color(req.getColor())
                .active(true)
                .tenantId(TenantContext.getTenantId())
                .build();
        cat = categoryRepository.save(cat);
        return toDto(cat);
    }

    @Transactional
    public ClassCategoryDto update(UUID id, CreateClassCategoryRequest req) {
        ClassCategory cat = categoryRepository.findById(id)
                .orElseThrow(() -> BusinessException.notFound("ClassCategory", id));
        cat.setName(req.getName());
        cat.setDescription(req.getDescription());
        cat.setColor(req.getColor());
        cat = categoryRepository.save(cat);
        return toDto(cat);
    }

    @Transactional
    public void deactivate(UUID id) {
        ClassCategory cat = categoryRepository.findById(id)
                .orElseThrow(() -> BusinessException.notFound("ClassCategory", id));
        cat.setActive(false);
        categoryRepository.save(cat);
    }

    ClassCategoryDto toDto(ClassCategory c) {
        return ClassCategoryDto.builder()
                .id(c.getId())
                .name(c.getName())
                .description(c.getDescription())
                .color(c.getColor())
                .active(c.isActive())
                .createdAt(c.getCreatedAt())
                .build();
    }
}

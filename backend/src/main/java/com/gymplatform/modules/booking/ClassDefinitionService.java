package com.gymplatform.modules.booking;

import com.gymplatform.config.multitenancy.TenantContext;
import com.gymplatform.modules.booking.dto.ClassDefinitionDto;
import com.gymplatform.modules.booking.dto.CreateClassRequest;
import com.gymplatform.shared.BusinessException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ClassDefinitionService {

    private final ClassDefinitionRepository classRepository;
    private final ClassCategoryRepository categoryRepository;

    public Page<ClassDefinitionDto> getAll(Pageable pageable) {
        return classRepository.findByActiveTrue(pageable).map(this::toDto);
    }

    public ClassDefinitionDto getById(UUID id) {
        ClassDefinition cls = classRepository.findById(id)
                .orElseThrow(() -> BusinessException.notFound("ClassDefinition", id));
        return toDto(cls);
    }

    @Transactional
    public ClassDefinitionDto create(CreateClassRequest req) {
        if (req.getCategoryId() != null) {
            categoryRepository.findById(req.getCategoryId())
                    .orElseThrow(() -> BusinessException.notFound("ClassCategory", req.getCategoryId()));
        }

        ClassDefinition cls = ClassDefinition.builder()
                .name(req.getName())
                .description(req.getDescription())
                .categoryId(req.getCategoryId())
                .trainerId(req.getTrainerId())
                .room(req.getRoom())
                .capacity(req.getCapacity())
                .durationMinutes(req.getDurationMinutes())
                .virtualLink(req.getVirtualLink())
                .allowWaitlist(req.isAllowWaitlist())
                .bookingCutoffMinutes(req.getBookingCutoffMinutes())
                .cancellationCutoffMinutes(req.getCancellationCutoffMinutes())
                .allowTrial(req.isAllowTrial())
                .active(true)
                .tenantId(TenantContext.getTenantId())
                .build();
        cls = classRepository.save(cls);
        return toDto(cls);
    }

    @Transactional
    public ClassDefinitionDto update(UUID id, CreateClassRequest req) {
        ClassDefinition cls = classRepository.findById(id)
                .orElseThrow(() -> BusinessException.notFound("ClassDefinition", id));

        cls.setName(req.getName());
        cls.setDescription(req.getDescription());
        cls.setCategoryId(req.getCategoryId());
        cls.setTrainerId(req.getTrainerId());
        cls.setRoom(req.getRoom());
        cls.setCapacity(req.getCapacity());
        cls.setDurationMinutes(req.getDurationMinutes());
        cls.setVirtualLink(req.getVirtualLink());
        cls.setAllowWaitlist(req.isAllowWaitlist());
        cls.setBookingCutoffMinutes(req.getBookingCutoffMinutes());
        cls.setCancellationCutoffMinutes(req.getCancellationCutoffMinutes());
        cls.setAllowTrial(req.isAllowTrial());

        cls = classRepository.save(cls);
        return toDto(cls);
    }

    @Transactional
    public void deactivate(UUID id) {
        ClassDefinition cls = classRepository.findById(id)
                .orElseThrow(() -> BusinessException.notFound("ClassDefinition", id));
        cls.setActive(false);
        classRepository.save(cls);
    }

    ClassDefinitionDto toDto(ClassDefinition c) {
        String categoryName = null;
        if (c.getCategoryId() != null) {
            categoryName = categoryRepository.findById(c.getCategoryId())
                    .map(ClassCategory::getName)
                    .orElse(null);
        }

        return ClassDefinitionDto.builder()
                .id(c.getId())
                .name(c.getName())
                .description(c.getDescription())
                .categoryId(c.getCategoryId())
                .categoryName(categoryName)
                .trainerId(c.getTrainerId())
                .room(c.getRoom())
                .capacity(c.getCapacity())
                .durationMinutes(c.getDurationMinutes())
                .virtualLink(c.getVirtualLink())
                .allowWaitlist(c.isAllowWaitlist())
                .bookingCutoffMinutes(c.getBookingCutoffMinutes())
                .cancellationCutoffMinutes(c.getCancellationCutoffMinutes())
                .allowTrial(c.isAllowTrial())
                .active(c.isActive())
                .createdAt(c.getCreatedAt())
                .build();
    }
}

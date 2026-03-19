package com.gymplatform.modules.training;

import com.gymplatform.config.multitenancy.TenantContext;
import com.gymplatform.modules.training.dto.CreateExerciseRequest;
import com.gymplatform.modules.training.dto.ExerciseDto;
import com.gymplatform.shared.AuditLogService;
import com.gymplatform.shared.BusinessException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class ExerciseService {

    private final ExerciseRepository exerciseRepository;
    private final AuditLogService auditLogService;

    public Page<ExerciseDto> searchExercises(String name, MuscleGroup muscleGroup,
                                              ExerciseType exerciseType, String equipment,
                                              Pageable pageable) {
        Specification<Exercise> spec = Specification
                .where(ExerciseSpecification.isActive())
                .and(ExerciseSpecification.isAvailableForTenant(TenantContext.getTenantId()))
                .and(ExerciseSpecification.hasName(name))
                .and(ExerciseSpecification.hasMuscleGroup(muscleGroup))
                .and(ExerciseSpecification.hasExerciseType(exerciseType))
                .and(ExerciseSpecification.hasEquipment(equipment));
        return exerciseRepository.findAll(spec, pageable).map(this::toDto);
    }

    public ExerciseDto getById(UUID id) {
        Exercise exercise = exerciseRepository.findById(id)
                .orElseThrow(() -> BusinessException.notFound("Exercise", id));
        return toDto(exercise);
    }

    @Transactional
    public ExerciseDto create(CreateExerciseRequest req, UUID userId) {
        Exercise exercise = Exercise.builder()
                .name(req.getName())
                .description(req.getDescription())
                .exerciseType(req.getExerciseType())
                .primaryMuscleGroup(req.getPrimaryMuscleGroup())
                .secondaryMuscleGroup(req.getSecondaryMuscleGroup())
                .equipment(req.getEquipment())
                .videoUrl(req.getVideoUrl())
                .thumbnailUrl(req.getThumbnailUrl())
                .executionTips(req.getExecutionTips())
                .postureNotes(req.getPostureNotes())
                .difficultyLevel(req.getDifficultyLevel())
                .active(true)
                .global(false)
                .tenantId(TenantContext.getTenantId())
                .build();
        exercise = exerciseRepository.save(exercise);
        auditLogService.log("Exercise", exercise.getId(), "CREATE", userId, null, null);
        return toDto(exercise);
    }

    @Transactional
    public ExerciseDto update(UUID id, CreateExerciseRequest req, UUID userId) {
        Exercise exercise = exerciseRepository.findById(id)
                .orElseThrow(() -> BusinessException.notFound("Exercise", id));
        exercise.setName(req.getName());
        exercise.setDescription(req.getDescription());
        exercise.setExerciseType(req.getExerciseType());
        exercise.setPrimaryMuscleGroup(req.getPrimaryMuscleGroup());
        exercise.setSecondaryMuscleGroup(req.getSecondaryMuscleGroup());
        exercise.setEquipment(req.getEquipment());
        exercise.setVideoUrl(req.getVideoUrl());
        exercise.setThumbnailUrl(req.getThumbnailUrl());
        exercise.setExecutionTips(req.getExecutionTips());
        exercise.setPostureNotes(req.getPostureNotes());
        exercise.setDifficultyLevel(req.getDifficultyLevel());
        exercise = exerciseRepository.save(exercise);
        auditLogService.log("Exercise", exercise.getId(), "UPDATE", userId, null, null);
        return toDto(exercise);
    }

    @Transactional
    public void deactivate(UUID id, UUID userId) {
        Exercise exercise = exerciseRepository.findById(id)
                .orElseThrow(() -> BusinessException.notFound("Exercise", id));
        exercise.setActive(false);
        exerciseRepository.save(exercise);
        auditLogService.log("Exercise", exercise.getId(), "DEACTIVATE", userId, null, null);
    }

    public List<String> getEquipmentList() {
        return exerciseRepository.findDistinctEquipment();
    }

    public List<ExerciseDto> suggest(String term) {
        if (term == null || term.length() < 2) return List.of();
        // Try full term first
        var results = exerciseRepository.suggestByName(term, org.springframework.data.domain.PageRequest.of(0, 5));
        if (!results.isEmpty()) return results.stream().map(this::toDto).toList();
        // If no results, try each word individually and intersect
        String[] words = term.trim().split("\\s+");
        if (words.length > 1) {
            // Search with the longest word first
            String longest = "";
            for (String w : words) if (w.length() > longest.length()) longest = w;
            var candidates = exerciseRepository.suggestByName(longest, org.springframework.data.domain.PageRequest.of(0, 20));
            String termLower = term.toLowerCase();
            return candidates.stream()
                    .filter(e -> {
                        String nameLower = e.getName().toLowerCase();
                        for (String w : words) {
                            if (w.length() >= 2 && !nameLower.contains(w.toLowerCase())) return false;
                        }
                        return true;
                    })
                    .limit(5)
                    .map(this::toDto).toList();
        }
        return List.of();
    }

    private ExerciseDto toDto(Exercise e) {
        return ExerciseDto.builder()
                .id(e.getId())
                .name(e.getName())
                .description(e.getDescription())
                .exerciseType(e.getExerciseType())
                .primaryMuscleGroup(e.getPrimaryMuscleGroup())
                .secondaryMuscleGroup(e.getSecondaryMuscleGroup())
                .equipment(e.getEquipment())
                .videoUrl(e.getVideoUrl())
                .thumbnailUrl(e.getThumbnailUrl())
                .executionTips(e.getExecutionTips())
                .postureNotes(e.getPostureNotes())
                .difficultyLevel(e.getDifficultyLevel())
                .active(e.isActive())
                .global(e.isGlobal())
                .createdAt(e.getCreatedAt())
                .build();
    }
}

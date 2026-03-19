package com.gymplatform.modules.training;

import com.gymplatform.config.multitenancy.TenantContext;
import com.gymplatform.modules.member.Member;
import com.gymplatform.modules.member.MemberRepository;
import com.gymplatform.modules.training.dto.*;
import com.gymplatform.shared.AuditLogService;
import com.gymplatform.shared.BusinessException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class TrainingPlanService {

    private final TrainingPlanRepository planRepository;
    private final TrainingPlanExerciseRepository planExerciseRepository;
    private final ExerciseRepository exerciseRepository;
    private final MemberRepository memberRepository;
    private final AuditLogService auditLogService;
    private final RabbitTemplate rabbitTemplate;

    @Transactional
    public TrainingPlanDto create(CreateTrainingPlanRequest req, UUID trainerId) {
        TrainingPlan plan = TrainingPlan.builder()
                .name(req.getName())
                .description(req.getDescription())
                .memberId(req.getMemberId())
                .trainerId(trainerId)
                .status(TrainingPlanStatus.DRAFT)
                .template(req.isTemplate())
                .catalog(req.isCatalog())
                .category(req.getCategory())
                .estimatedDurationMinutes(req.getEstimatedDurationMinutes())
                .difficultyLevel(req.getDifficultyLevel())
                .tenantId(TenantContext.getTenantId())
                .build();
        plan = planRepository.save(plan);

        if (req.getExercises() != null && !req.getExercises().isEmpty()) {
            saveExercises(plan.getId(), req.getExercises());
        }

        auditLogService.log("TrainingPlan", plan.getId(), "CREATE", trainerId, null, null);
        return toPlanDto(plan, true);
    }

    @Transactional
    public TrainingPlanDto update(UUID planId, CreateTrainingPlanRequest req, UUID userId) {
        TrainingPlan plan = planRepository.findById(planId)
                .orElseThrow(() -> BusinessException.notFound("TrainingPlan", planId));

        plan.setName(req.getName());
        plan.setDescription(req.getDescription());
        plan.setMemberId(req.getMemberId());
        plan.setTemplate(req.isTemplate());
        plan.setCatalog(req.isCatalog());
        plan.setCategory(req.getCategory());
        plan.setEstimatedDurationMinutes(req.getEstimatedDurationMinutes());
        plan.setDifficultyLevel(req.getDifficultyLevel());
        plan = planRepository.save(plan);

        if (req.getExercises() != null) {
            planExerciseRepository.deleteByPlanId(planId);
            saveExercises(planId, req.getExercises());
        }

        auditLogService.log("TrainingPlan", plan.getId(), "UPDATE", userId, null, null);
        return toPlanDto(plan, true);
    }

    @Transactional
    public TrainingPlanDto publish(UUID planId, UUID userId) {
        TrainingPlan plan = planRepository.findById(planId)
                .orElseThrow(() -> BusinessException.notFound("TrainingPlan", planId));

        if (plan.getStatus() != TrainingPlanStatus.DRAFT) {
            throw BusinessException.badRequest("Only draft plans can be published");
        }

        int exerciseCount = planExerciseRepository.countByPlanId(planId);
        if (exerciseCount == 0) {
            throw BusinessException.badRequest("Cannot publish a plan with no exercises");
        }

        plan.setStatus(TrainingPlanStatus.PUBLISHED);
        plan = planRepository.save(plan);

        auditLogService.log("TrainingPlan", plan.getId(), "PUBLISH", userId, null, null);
        publishPlanEvent(plan, "training.plan.published");

        return toPlanDto(plan, true);
    }

    @Transactional
    public TrainingPlanDto archive(UUID planId, UUID userId) {
        TrainingPlan plan = planRepository.findById(planId)
                .orElseThrow(() -> BusinessException.notFound("TrainingPlan", planId));

        plan.setStatus(TrainingPlanStatus.ARCHIVED);
        plan = planRepository.save(plan);

        auditLogService.log("TrainingPlan", plan.getId(), "ARCHIVE", userId, null, null);
        return toPlanDto(plan, false);
    }

    @Transactional
    public TrainingPlanDto createFromTemplate(UUID templateId, UUID memberId, UUID trainerId) {
        TrainingPlan template = planRepository.findById(templateId)
                .orElseThrow(() -> BusinessException.notFound("TrainingPlan", templateId));

        if (!template.isTemplate() && !template.isCatalog()) {
            throw BusinessException.badRequest("Source plan is not a template or catalog plan");
        }

        TrainingPlan newPlan = TrainingPlan.builder()
                .name(template.getName())
                .description(template.getDescription())
                .memberId(memberId)
                .trainerId(trainerId)
                .status(TrainingPlanStatus.DRAFT)
                .template(false)
                .catalog(false)
                .category(template.getCategory())
                .estimatedDurationMinutes(template.getEstimatedDurationMinutes())
                .difficultyLevel(template.getDifficultyLevel())
                .tenantId(TenantContext.getTenantId())
                .build();
        newPlan = planRepository.save(newPlan);

        List<TrainingPlanExercise> templateExercises =
                planExerciseRepository.findByPlanIdOrderBySortOrderAsc(templateId);
        for (TrainingPlanExercise te : templateExercises) {
            TrainingPlanExercise copy = TrainingPlanExercise.builder()
                    .planId(newPlan.getId())
                    .exerciseId(te.getExerciseId())
                    .sortOrder(te.getSortOrder())
                    .sets(te.getSets())
                    .reps(te.getReps())
                    .weight(te.getWeight())
                    .restSeconds(te.getRestSeconds())
                    .trainerComment(te.getTrainerComment())
                    .supersetGroup(te.getSupersetGroup())
                    .tenantId(TenantContext.getTenantId())
                    .build();
            planExerciseRepository.save(copy);
        }

        auditLogService.log("TrainingPlan", newPlan.getId(), "CREATE_FROM_TEMPLATE", trainerId, null,
                "template:" + templateId);
        return toPlanDto(newPlan, true);
    }

    @Transactional
    public void reorderExercises(UUID planId, List<UUID> exerciseIds, UUID userId) {
        TrainingPlan plan = planRepository.findById(planId)
                .orElseThrow(() -> BusinessException.notFound("TrainingPlan", planId));

        List<TrainingPlanExercise> exercises =
                planExerciseRepository.findByPlanIdOrderBySortOrderAsc(planId);
        Map<UUID, TrainingPlanExercise> exerciseMap = exercises.stream()
                .collect(Collectors.toMap(TrainingPlanExercise::getId, e -> e));

        for (int i = 0; i < exerciseIds.size(); i++) {
            TrainingPlanExercise pe = exerciseMap.get(exerciseIds.get(i));
            if (pe != null) {
                pe.setSortOrder(i);
                planExerciseRepository.save(pe);
            }
        }

        auditLogService.log("TrainingPlan", planId, "REORDER", userId, null, null);
    }

    public TrainingPlanDto getById(UUID planId) {
        TrainingPlan plan = planRepository.findById(planId)
                .orElseThrow(() -> BusinessException.notFound("TrainingPlan", planId));
        return toPlanDto(plan, true);
    }

    public Page<TrainingPlanDto> getMemberPlans(UUID memberId, Pageable pageable) {
        return planRepository.findByMemberIdOrderByCreatedAtDesc(memberId, pageable)
                .map(p -> toPlanDto(p, false));
    }

    public Page<TrainingPlanDto> getTemplates(Pageable pageable) {
        return planRepository.findByTemplateTrueOrderByNameAsc(pageable)
                .map(p -> toPlanDto(p, false));
    }

    public Page<TrainingPlanDto> getCatalog(Pageable pageable) {
        return planRepository.findByCatalogTrueAndStatusOrderByNameAsc(TrainingPlanStatus.PUBLISHED, pageable)
                .map(p -> toPlanDto(p, false));
    }

    private void saveExercises(UUID planId, List<PlanExerciseRequest> exercises) {
        for (int i = 0; i < exercises.size(); i++) {
            PlanExerciseRequest req = exercises.get(i);
            exerciseRepository.findById(req.getExerciseId())
                    .orElseThrow(() -> BusinessException.notFound("Exercise", req.getExerciseId()));

            TrainingPlanExercise pe = TrainingPlanExercise.builder()
                    .planId(planId)
                    .exerciseId(req.getExerciseId())
                    .sortOrder(req.getSortOrder() > 0 ? req.getSortOrder() : i)
                    .sets(req.getSets())
                    .reps(req.getReps())
                    .weight(req.getWeight())
                    .restSeconds(req.getRestSeconds())
                    .trainerComment(req.getTrainerComment())
                    .supersetGroup(req.getSupersetGroup())
                    .tenantId(TenantContext.getTenantId())
                    .build();
            planExerciseRepository.save(pe);
        }
    }

    private void publishPlanEvent(TrainingPlan plan, String routingKey) {
        try {
            Map<String, Object> event = Map.of(
                    "planId", plan.getId().toString(),
                    "planName", plan.getName(),
                    "memberId", plan.getMemberId() != null ? plan.getMemberId().toString() : "",
                    "trainerId", plan.getTrainerId() != null ? plan.getTrainerId().toString() : "",
                    "tenantId", TenantContext.getTenantId(),
                    "timestamp", Instant.now().toString()
            );
            rabbitTemplate.convertAndSend("member.events", routingKey, event);
        } catch (Exception e) {
            log.warn("Failed to publish training plan event: {}", e.getMessage());
        }
    }

    private TrainingPlanDto toPlanDto(TrainingPlan plan, boolean includeExercises) {
        String memberName = plan.getMemberId() != null ? getMemberName(plan.getMemberId()) : null;
        String trainerName = plan.getTrainerId() != null ? getMemberName(plan.getTrainerId()) : null;

        List<TrainingPlanExerciseDto> exerciseDtos = null;
        int exerciseCount = 0;
        if (includeExercises) {
            List<TrainingPlanExercise> exercises =
                    planExerciseRepository.findByPlanIdOrderBySortOrderAsc(plan.getId());
            exerciseCount = exercises.size();
            exerciseDtos = exercises.stream().map(this::toPlanExerciseDto).collect(Collectors.toList());
        } else {
            exerciseCount = planExerciseRepository.countByPlanId(plan.getId());
        }

        return TrainingPlanDto.builder()
                .id(plan.getId())
                .name(plan.getName())
                .description(plan.getDescription())
                .memberId(plan.getMemberId())
                .memberName(memberName)
                .trainerId(plan.getTrainerId())
                .trainerName(trainerName)
                .status(plan.getStatus())
                .template(plan.isTemplate())
                .catalog(plan.isCatalog())
                .category(plan.getCategory())
                .estimatedDurationMinutes(plan.getEstimatedDurationMinutes())
                .difficultyLevel(plan.getDifficultyLevel())
                .exerciseCount(exerciseCount)
                .exercises(exerciseDtos)
                .createdAt(plan.getCreatedAt())
                .updatedAt(plan.getUpdatedAt())
                .build();
    }

    private TrainingPlanExerciseDto toPlanExerciseDto(TrainingPlanExercise pe) {
        Exercise exercise = exerciseRepository.findById(pe.getExerciseId()).orElse(null);
        return TrainingPlanExerciseDto.builder()
                .id(pe.getId())
                .exerciseId(pe.getExerciseId())
                .exerciseName(exercise != null ? exercise.getName() : null)
                .exerciseThumbnailUrl(exercise != null ? exercise.getThumbnailUrl() : null)
                .primaryMuscleGroup(exercise != null ? exercise.getPrimaryMuscleGroup().name() : null)
                .sortOrder(pe.getSortOrder())
                .sets(pe.getSets())
                .reps(pe.getReps())
                .weight(pe.getWeight())
                .restSeconds(pe.getRestSeconds())
                .trainerComment(pe.getTrainerComment())
                .supersetGroup(pe.getSupersetGroup())
                .build();
    }

    private String getMemberName(UUID userId) {
        return memberRepository.findById(userId)
                .map(m -> m.getFirstName() + " " + m.getLastName())
                .orElse(null);
    }
}

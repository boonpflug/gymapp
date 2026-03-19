package com.gymplatform.modules.training;

import com.gymplatform.config.multitenancy.TenantContext;
import com.gymplatform.modules.training.dto.CreateGoalRequest;
import com.gymplatform.modules.training.dto.TrainingGoalDto;
import com.gymplatform.modules.training.dto.UpdateGoalProgressRequest;
import com.gymplatform.shared.AuditLogService;
import com.gymplatform.shared.BusinessException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class TrainingGoalService {

    private final TrainingGoalRepository goalRepository;
    private final AuditLogService auditLogService;

    @Transactional
    public TrainingGoalDto create(CreateGoalRequest req, UUID userId) {
        TrainingGoal goal = TrainingGoal.builder()
                .memberId(req.getMemberId())
                .goalType(req.getGoalType())
                .title(req.getTitle())
                .description(req.getDescription())
                .targetValue(req.getTargetValue())
                .currentValue(req.getCurrentValue() != null ? req.getCurrentValue() : BigDecimal.ZERO)
                .unit(req.getUnit())
                .startDate(req.getStartDate() != null ? req.getStartDate() : LocalDate.now())
                .targetDate(req.getTargetDate())
                .status(GoalStatus.ACTIVE)
                .tenantId(TenantContext.getTenantId())
                .build();
        goal = goalRepository.save(goal);
        auditLogService.log("TrainingGoal", goal.getId(), "CREATE", userId, null, null);
        return toDto(goal);
    }

    @Transactional
    public TrainingGoalDto updateProgress(UUID goalId, UpdateGoalProgressRequest req, UUID userId) {
        TrainingGoal goal = goalRepository.findById(goalId)
                .orElseThrow(() -> BusinessException.notFound("TrainingGoal", goalId));

        if (goal.getStatus() != GoalStatus.ACTIVE) {
            throw BusinessException.badRequest("Can only update progress on active goals");
        }

        goal.setCurrentValue(req.getCurrentValue());

        if (goal.getTargetValue() != null && req.getCurrentValue() != null
                && req.getCurrentValue().compareTo(goal.getTargetValue()) >= 0) {
            goal.setStatus(GoalStatus.ACHIEVED);
            auditLogService.log("TrainingGoal", goal.getId(), "ACHIEVED", userId, null, null);
        }

        goal = goalRepository.save(goal);
        auditLogService.log("TrainingGoal", goal.getId(), "UPDATE_PROGRESS", userId, null,
                req.getCurrentValue().toString());
        return toDto(goal);
    }

    @Transactional
    public TrainingGoalDto abandon(UUID goalId, UUID userId) {
        TrainingGoal goal = goalRepository.findById(goalId)
                .orElseThrow(() -> BusinessException.notFound("TrainingGoal", goalId));
        goal.setStatus(GoalStatus.ABANDONED);
        goal = goalRepository.save(goal);
        auditLogService.log("TrainingGoal", goal.getId(), "ABANDON", userId, null, null);
        return toDto(goal);
    }

    public List<TrainingGoalDto> getMemberGoals(UUID memberId) {
        return goalRepository.findByMemberIdOrderByCreatedAtDesc(memberId)
                .stream().map(this::toDto).collect(Collectors.toList());
    }

    public List<TrainingGoalDto> getMemberActiveGoals(UUID memberId) {
        return goalRepository.findByMemberIdAndStatusOrderByCreatedAtDesc(memberId, GoalStatus.ACTIVE)
                .stream().map(this::toDto).collect(Collectors.toList());
    }

    private TrainingGoalDto toDto(TrainingGoal g) {
        Double progressPercent = null;
        if (g.getTargetValue() != null && g.getTargetValue().compareTo(BigDecimal.ZERO) > 0
                && g.getCurrentValue() != null) {
            progressPercent = g.getCurrentValue()
                    .divide(g.getTargetValue(), 4, RoundingMode.HALF_UP)
                    .multiply(BigDecimal.valueOf(100))
                    .doubleValue();
        }

        return TrainingGoalDto.builder()
                .id(g.getId())
                .memberId(g.getMemberId())
                .goalType(g.getGoalType())
                .title(g.getTitle())
                .description(g.getDescription())
                .targetValue(g.getTargetValue())
                .currentValue(g.getCurrentValue())
                .unit(g.getUnit())
                .startDate(g.getStartDate())
                .targetDate(g.getTargetDate())
                .status(g.getStatus())
                .progressPercent(progressPercent)
                .createdAt(g.getCreatedAt())
                .updatedAt(g.getUpdatedAt())
                .build();
    }
}

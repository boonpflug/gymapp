package com.gymplatform.modules.training.dto;

import com.gymplatform.modules.training.GoalStatus;
import com.gymplatform.modules.training.GoalType;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

@Data
@Builder
public class TrainingGoalDto {
    private UUID id;
    private UUID memberId;
    private GoalType goalType;
    private String title;
    private String description;
    private BigDecimal targetValue;
    private BigDecimal currentValue;
    private String unit;
    private LocalDate startDate;
    private LocalDate targetDate;
    private GoalStatus status;
    private Double progressPercent;
    private Instant createdAt;
    private Instant updatedAt;
}

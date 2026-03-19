package com.gymplatform.modules.training.dto;

import com.gymplatform.modules.training.GoalType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

@Data
public class CreateGoalRequest {
    @NotNull(message = "Member ID is required")
    private UUID memberId;
    @NotNull(message = "Goal type is required")
    private GoalType goalType;
    @NotBlank(message = "Goal title is required")
    private String title;
    private String description;
    private BigDecimal targetValue;
    private BigDecimal currentValue;
    private String unit;
    private LocalDate startDate;
    private LocalDate targetDate;
}

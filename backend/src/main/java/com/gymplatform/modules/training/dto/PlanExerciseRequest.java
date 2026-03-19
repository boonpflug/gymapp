package com.gymplatform.modules.training.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;
import java.util.UUID;

@Data
public class PlanExerciseRequest {
    @NotNull(message = "Exercise ID is required")
    private UUID exerciseId;
    private int sortOrder;
    @Min(value = 1, message = "Sets must be at least 1")
    private int sets = 3;
    @Min(value = 1, message = "Reps must be at least 1")
    private int reps = 10;
    private BigDecimal weight;
    private Integer restSeconds;
    private String trainerComment;
    private Integer supersetGroup;
}

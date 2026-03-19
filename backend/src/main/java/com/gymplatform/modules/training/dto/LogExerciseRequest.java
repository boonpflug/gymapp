package com.gymplatform.modules.training.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;
import java.util.UUID;

@Data
public class LogExerciseRequest {
    @NotNull(message = "Session ID is required")
    private UUID sessionId;
    @NotNull(message = "Exercise ID is required")
    private UUID exerciseId;
    private UUID planExerciseId;
    private int setNumber;
    private Integer targetReps;
    private Integer actualReps;
    private BigDecimal targetWeight;
    private BigDecimal actualWeight;
    private Integer durationSeconds;
    private String notes;
    private boolean completed = true;
}

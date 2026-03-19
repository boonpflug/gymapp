package com.gymplatform.modules.training.dto;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Data
@Builder
public class TrainingLogDto {
    private UUID id;
    private UUID sessionId;
    private UUID exerciseId;
    private String exerciseName;
    private UUID planExerciseId;
    private int setNumber;
    private Integer targetReps;
    private Integer actualReps;
    private BigDecimal targetWeight;
    private BigDecimal actualWeight;
    private Integer durationSeconds;
    private String notes;
    private boolean completed;
    private Instant createdAt;
}

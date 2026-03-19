package com.gymplatform.modules.training.dto;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.util.UUID;

@Data
@Builder
public class TrainingPlanExerciseDto {
    private UUID id;
    private UUID exerciseId;
    private String exerciseName;
    private String exerciseThumbnailUrl;
    private String primaryMuscleGroup;
    private int sortOrder;
    private int sets;
    private int reps;
    private BigDecimal weight;
    private Integer restSeconds;
    private String trainerComment;
    private Integer supersetGroup;
}

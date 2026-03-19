package com.gymplatform.modules.training.dto;

import com.gymplatform.modules.training.TrainingPlanStatus;
import lombok.Builder;
import lombok.Data;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Data
@Builder
public class TrainingPlanDto {
    private UUID id;
    private String name;
    private String description;
    private UUID memberId;
    private String memberName;
    private UUID trainerId;
    private String trainerName;
    private TrainingPlanStatus status;
    private boolean template;
    private boolean catalog;
    private String category;
    private Integer estimatedDurationMinutes;
    private String difficultyLevel;
    private int exerciseCount;
    private List<TrainingPlanExerciseDto> exercises;
    private Instant createdAt;
    private Instant updatedAt;
}

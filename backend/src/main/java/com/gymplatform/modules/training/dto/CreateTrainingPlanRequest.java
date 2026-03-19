package com.gymplatform.modules.training.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.util.List;
import java.util.UUID;

@Data
public class CreateTrainingPlanRequest {
    @NotBlank(message = "Plan name is required")
    private String name;
    private String description;
    private UUID memberId;
    private boolean template;
    private boolean catalog;
    private String category;
    private Integer estimatedDurationMinutes;
    private String difficultyLevel;
    private List<PlanExerciseRequest> exercises;
}

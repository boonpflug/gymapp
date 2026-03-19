package com.gymplatform.modules.training.dto;

import com.gymplatform.modules.training.ExerciseType;
import com.gymplatform.modules.training.MuscleGroup;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class CreateExerciseRequest {
    @NotBlank(message = "Exercise name is required")
    private String name;
    private String description;
    @NotNull(message = "Exercise type is required")
    private ExerciseType exerciseType;
    @NotNull(message = "Primary muscle group is required")
    private MuscleGroup primaryMuscleGroup;
    private MuscleGroup secondaryMuscleGroup;
    private String equipment;
    private String videoUrl;
    private String thumbnailUrl;
    private String executionTips;
    private String postureNotes;
    private String difficultyLevel;
}

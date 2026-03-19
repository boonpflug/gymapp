package com.gymplatform.modules.training.dto;

import com.gymplatform.modules.training.ExerciseType;
import com.gymplatform.modules.training.MuscleGroup;
import lombok.Builder;
import lombok.Data;

import java.time.Instant;
import java.util.UUID;

@Data
@Builder
public class ExerciseDto {
    private UUID id;
    private String name;
    private String description;
    private ExerciseType exerciseType;
    private MuscleGroup primaryMuscleGroup;
    private MuscleGroup secondaryMuscleGroup;
    private String equipment;
    private String videoUrl;
    private String thumbnailUrl;
    private String executionTips;
    private String postureNotes;
    private String difficultyLevel;
    private boolean active;
    private boolean global;
    private Instant createdAt;
}

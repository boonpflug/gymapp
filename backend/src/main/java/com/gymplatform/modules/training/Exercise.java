package com.gymplatform.modules.training;

import com.gymplatform.shared.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "exercises")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Exercise extends BaseEntity {

    @Column(nullable = false, length = 200)
    private String name;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(name = "exercise_type", nullable = false, length = 30)
    private ExerciseType exerciseType;

    @Enumerated(EnumType.STRING)
    @Column(name = "primary_muscle_group", nullable = false, length = 30)
    private MuscleGroup primaryMuscleGroup;

    @Enumerated(EnumType.STRING)
    @Column(name = "secondary_muscle_group", length = 30)
    private MuscleGroup secondaryMuscleGroup;

    @Column(length = 100)
    private String equipment;

    @Column(name = "video_url", length = 500)
    private String videoUrl;

    @Column(name = "thumbnail_url", length = 500)
    private String thumbnailUrl;

    @Column(name = "execution_tips", columnDefinition = "TEXT")
    private String executionTips;

    @Column(name = "posture_notes", columnDefinition = "TEXT")
    private String postureNotes;

    @Column(name = "difficulty_level", length = 20)
    private String difficultyLevel;

    @Column(nullable = false)
    private boolean active = true;

    @Column(name = "is_global")
    private boolean global = true;

    @Column(name = "tenant_id", length = 100)
    private String tenantId;
}

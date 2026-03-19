package com.gymplatform.modules.training;

import com.gymplatform.shared.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.util.UUID;

@Entity
@Table(name = "training_plan_exercises")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class TrainingPlanExercise extends BaseEntity {

    @Column(name = "plan_id", nullable = false)
    private UUID planId;

    @Column(name = "exercise_id", nullable = false)
    private UUID exerciseId;

    @Column(name = "sort_order", nullable = false)
    private int sortOrder;

    @Column(nullable = false)
    private int sets;

    @Column(nullable = false)
    private int reps;

    @Column(precision = 10, scale = 2)
    private BigDecimal weight;

    @Column(name = "rest_seconds")
    private Integer restSeconds;

    @Column(name = "trainer_comment", columnDefinition = "TEXT")
    private String trainerComment;

    @Column(name = "superset_group")
    private Integer supersetGroup;

    @Column(name = "tenant_id", length = 100)
    private String tenantId;
}

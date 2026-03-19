package com.gymplatform.modules.training;

import com.gymplatform.shared.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.util.UUID;

@Entity
@Table(name = "training_logs")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class TrainingLog extends BaseEntity {

    @Column(name = "session_id", nullable = false)
    private UUID sessionId;

    @Column(name = "exercise_id", nullable = false)
    private UUID exerciseId;

    @Column(name = "plan_exercise_id")
    private UUID planExerciseId;

    @Column(name = "set_number", nullable = false)
    private int setNumber;

    @Column(name = "target_reps")
    private Integer targetReps;

    @Column(name = "actual_reps")
    private Integer actualReps;

    @Column(name = "target_weight", precision = 10, scale = 2)
    private BigDecimal targetWeight;

    @Column(name = "actual_weight", precision = 10, scale = 2)
    private BigDecimal actualWeight;

    @Column(name = "duration_seconds")
    private Integer durationSeconds;

    @Column(columnDefinition = "TEXT")
    private String notes;

    @Column(nullable = false)
    private boolean completed = false;

    @Column(name = "tenant_id", length = 100)
    private String tenantId;
}

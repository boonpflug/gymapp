package com.gymplatform.modules.training;

import com.gymplatform.shared.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

import java.util.UUID;

@Entity
@Table(name = "training_plans")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class TrainingPlan extends BaseEntity {

    @Column(nullable = false, length = 200)
    private String name;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(name = "member_id")
    private UUID memberId;

    @Column(name = "trainer_id")
    private UUID trainerId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private TrainingPlanStatus status;

    @Column(name = "is_template")
    private boolean template = false;

    @Column(name = "is_catalog")
    private boolean catalog = false;

    @Column(name = "category", length = 100)
    private String category;

    @Column(name = "estimated_duration_minutes")
    private Integer estimatedDurationMinutes;

    @Column(name = "difficulty_level", length = 20)
    private String difficultyLevel;

    @Column(name = "tenant_id", length = 100)
    private String tenantId;
}

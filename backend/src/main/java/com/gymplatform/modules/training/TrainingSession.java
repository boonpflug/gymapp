package com.gymplatform.modules.training;

import com.gymplatform.shared.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "training_sessions")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class TrainingSession extends BaseEntity {

    @Column(name = "member_id", nullable = false)
    private UUID memberId;

    @Column(name = "plan_id")
    private UUID planId;

    @Column(name = "started_at", nullable = false)
    private Instant startedAt;

    @Column(name = "finished_at")
    private Instant finishedAt;

    @Column(name = "duration_minutes")
    private Integer durationMinutes;

    @Column(columnDefinition = "TEXT")
    private String notes;

    @Column(name = "rating")
    private Integer rating;

    @Column(name = "tenant_id", length = 100)
    private String tenantId;
}

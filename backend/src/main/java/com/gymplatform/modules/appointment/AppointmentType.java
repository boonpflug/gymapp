package com.gymplatform.modules.appointment;

import com.gymplatform.shared.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "appointment_types")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class AppointmentType extends BaseEntity {

    @Column(nullable = false, length = 100)
    private String name;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(name = "duration_minutes", nullable = false)
    private int durationMinutes;

    @Column(length = 20)
    private String color;

    @Column(name = "requires_trainer", nullable = false)
    private boolean requiresTrainer;

    @Column(nullable = false)
    private boolean active;

    @Column(name = "tenant_id", length = 100)
    private String tenantId;
}

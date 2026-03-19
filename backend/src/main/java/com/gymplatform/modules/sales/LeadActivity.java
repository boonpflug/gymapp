package com.gymplatform.modules.sales;

import com.gymplatform.shared.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "lead_activities")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class LeadActivity extends BaseEntity {

    @Column(name = "lead_id", nullable = false)
    private UUID leadId;

    @Enumerated(EnumType.STRING)
    @Column(name = "activity_type", nullable = false, length = 30)
    private LeadActivityType activityType;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(length = 200)
    private String outcome;

    @Column(name = "staff_id")
    private UUID staffId;

    @Column(name = "due_date")
    private LocalDate dueDate;

    @Column(name = "completed_at")
    private Instant completedAt;

    @Column(name = "tenant_id", length = 100)
    private String tenantId;
}

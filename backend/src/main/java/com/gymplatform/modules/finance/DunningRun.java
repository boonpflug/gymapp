package com.gymplatform.modules.finance;

import com.gymplatform.shared.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "dunning_runs")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class DunningRun extends BaseEntity {

    @Column(name = "invoice_id", nullable = false)
    private UUID invoiceId;

    @Column(name = "member_id", nullable = false)
    private UUID memberId;

    @Column(name = "current_level")
    private int currentLevel = 1;

    @Column(name = "last_escalated_at")
    private Instant lastEscalatedAt;

    @Column
    private boolean resolved = false;

    @Column(name = "tenant_id")
    private String tenantId;
}

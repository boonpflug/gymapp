package com.gymplatform.modules.finance;

import com.gymplatform.shared.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

import java.util.UUID;

@Entity
@Table(name = "dunning_levels")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class DunningLevel extends BaseEntity {

    @Column(nullable = false)
    private int level;

    private String name;

    @Column(name = "days_after_due", nullable = false)
    private int daysAfterDue;

    @Column
    private String action;

    @Column(name = "template_id")
    private UUID templateId;

    @Column(name = "tenant_id")
    private String tenantId;
}

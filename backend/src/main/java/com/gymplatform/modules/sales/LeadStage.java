package com.gymplatform.modules.sales;

import com.gymplatform.shared.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "lead_stages")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class LeadStage extends BaseEntity {

    @Column(nullable = false, length = 100)
    private String name;

    @Column(name = "sort_order", nullable = false)
    private int sortOrder;

    @Column(length = 7)
    private String color;

    @Column(name = "is_default")
    private boolean isDefault = false;

    @Column(name = "is_closed")
    private boolean isClosed = false;

    @Column(name = "is_won")
    private boolean isWon = false;

    @Column(name = "tenant_id", length = 100)
    private String tenantId;
}

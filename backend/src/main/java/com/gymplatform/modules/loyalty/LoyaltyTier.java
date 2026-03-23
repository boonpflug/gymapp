package com.gymplatform.modules.loyalty;

import com.gymplatform.shared.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "loyalty_tiers")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class LoyaltyTier extends BaseEntity {

    @Column(nullable = false, length = 100)
    private String name;

    @Column(name = "min_points", nullable = false)
    private int minPoints;

    @Column(length = 20)
    private String color;

    @Column(length = 100)
    private String icon;

    @Column(columnDefinition = "TEXT")
    private String perks;

    @Column(name = "sort_order")
    private int sortOrder;

    @Column(nullable = false)
    private boolean active;

    @Column(name = "tenant_id", length = 100)
    private String tenantId;
}

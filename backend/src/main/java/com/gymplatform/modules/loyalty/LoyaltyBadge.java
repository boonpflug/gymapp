package com.gymplatform.modules.loyalty;

import com.gymplatform.shared.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "loyalty_badges")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class LoyaltyBadge extends BaseEntity {

    @Column(nullable = false, length = 100)
    private String name;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(length = 100)
    private String icon;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private BadgeCategory category;

    @Enumerated(EnumType.STRING)
    @Column(name = "criteria_type", nullable = false, length = 30)
    private BadgeCriteriaType criteriaType;

    @Column(name = "criteria_value")
    private Integer criteriaValue;

    @Column(nullable = false)
    private boolean active;

    @Column(name = "tenant_id", length = 100)
    private String tenantId;
}

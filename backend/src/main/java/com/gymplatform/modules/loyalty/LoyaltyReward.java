package com.gymplatform.modules.loyalty;

import com.gymplatform.shared.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;

@Entity
@Table(name = "loyalty_rewards")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class LoyaltyReward extends BaseEntity {

    @Column(nullable = false, length = 200)
    private String name;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(name = "reward_type", nullable = false, length = 20)
    private RewardType rewardType;

    @Column(name = "points_cost", nullable = false)
    private int pointsCost;

    @Column(precision = 10, scale = 2)
    private BigDecimal value;

    @Column(nullable = false)
    private boolean active;

    @Column(name = "image_url", length = 500)
    private String imageUrl;

    @Column(name = "max_redemptions_per_member")
    private Integer maxRedemptionsPerMember;

    @Column(name = "total_available")
    private Integer totalAvailable;

    @Column(name = "total_redeemed", nullable = false)
    private int totalRedeemed;

    @Column(name = "tenant_id", length = 100)
    private String tenantId;
}

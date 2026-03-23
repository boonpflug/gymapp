package com.gymplatform.modules.loyalty;

import com.gymplatform.shared.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "loyalty_redemptions")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class LoyaltyRedemption extends BaseEntity {

    @Column(name = "member_id", nullable = false)
    private UUID memberId;

    @Column(name = "reward_id", nullable = false)
    private UUID rewardId;

    @Column(name = "transaction_id")
    private UUID transactionId;

    @Column(name = "points_spent", nullable = false)
    private int pointsSpent;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private RedemptionStatus status;

    @Column(name = "fulfilled_at")
    private Instant fulfilledAt;

    @Column(columnDefinition = "TEXT")
    private String notes;

    @Column(name = "tenant_id", length = 100)
    private String tenantId;
}

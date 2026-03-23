package com.gymplatform.modules.loyalty.dto;

import com.gymplatform.modules.loyalty.RedemptionStatus;
import lombok.Builder;
import lombok.Data;

import java.time.Instant;
import java.util.UUID;

@Data
@Builder
public class LoyaltyRedemptionDto {
    private UUID id;
    private UUID memberId;
    private String memberName;
    private UUID rewardId;
    private String rewardName;
    private UUID transactionId;
    private int pointsSpent;
    private RedemptionStatus status;
    private Instant fulfilledAt;
    private String notes;
    private Instant createdAt;
}

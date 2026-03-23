package com.gymplatform.modules.loyalty.dto;

import com.gymplatform.modules.loyalty.RewardType;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Data
@Builder
public class LoyaltyRewardDto {
    private UUID id;
    private String name;
    private String description;
    private RewardType rewardType;
    private int pointsCost;
    private BigDecimal value;
    private boolean active;
    private String imageUrl;
    private Integer maxRedemptionsPerMember;
    private Integer totalAvailable;
    private int totalRedeemed;
    private Instant createdAt;
}

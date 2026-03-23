package com.gymplatform.modules.loyalty.dto;

import lombok.Builder;
import lombok.Data;

import java.util.List;
import java.util.UUID;

@Data
@Builder
public class MemberLoyaltySummaryDto {
    private UUID memberId;
    private int currentPoints;
    private int totalEarned;
    private LoyaltyTierDto currentTier;
    private LoyaltyTierDto nextTier;
    private int pointsToNextTier;
    private List<MemberStreakDto> streaks;
    private List<MemberBadgeDto> badges;
    private int totalBadges;
    private int totalRedemptions;
}

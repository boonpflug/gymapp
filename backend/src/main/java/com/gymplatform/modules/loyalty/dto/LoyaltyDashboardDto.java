package com.gymplatform.modules.loyalty.dto;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class LoyaltyDashboardDto {
    private int pointsIssuedThisMonth;
    private int pointsRedeemedThisMonth;
    private long redemptionsThisMonth;
    private long totalParticipants;
    private List<TopMemberDto> topMembers;
}

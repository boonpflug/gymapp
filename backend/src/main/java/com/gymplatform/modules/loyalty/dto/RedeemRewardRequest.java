package com.gymplatform.modules.loyalty.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.UUID;

@Data
public class RedeemRewardRequest {
    @NotNull(message = "Member ID is required")
    private UUID memberId;
    @NotNull(message = "Reward ID is required")
    private UUID rewardId;
}

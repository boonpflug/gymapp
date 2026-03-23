package com.gymplatform.modules.loyalty.dto;

import com.gymplatform.modules.loyalty.RewardType;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class CreateRewardRequest {
    @NotBlank(message = "Reward name is required")
    private String name;
    private String description;
    @NotNull(message = "Reward type is required")
    private RewardType rewardType;
    @Min(1)
    private int pointsCost;
    private BigDecimal value;
    private String imageUrl;
    private Integer maxRedemptionsPerMember;
    private Integer totalAvailable;
}

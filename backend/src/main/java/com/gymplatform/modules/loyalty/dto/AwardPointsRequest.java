package com.gymplatform.modules.loyalty.dto;

import com.gymplatform.modules.loyalty.LoyaltyAction;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.UUID;

@Data
public class AwardPointsRequest {
    @NotNull(message = "Member ID is required")
    private UUID memberId;
    @Min(1)
    private int points;
    @NotNull(message = "Loyalty action is required")
    private LoyaltyAction action;
    private UUID referenceId;
    private String description;
}

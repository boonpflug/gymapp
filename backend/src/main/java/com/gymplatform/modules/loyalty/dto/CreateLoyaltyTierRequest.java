package com.gymplatform.modules.loyalty.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class CreateLoyaltyTierRequest {
    @NotBlank(message = "Tier name is required")
    private String name;
    @Min(0)
    private int minPoints;
    private String color;
    private String icon;
    private String perks;
    private int sortOrder;
}

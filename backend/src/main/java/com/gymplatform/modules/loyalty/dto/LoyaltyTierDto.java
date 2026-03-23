package com.gymplatform.modules.loyalty.dto;

import lombok.Builder;
import lombok.Data;

import java.time.Instant;
import java.util.UUID;

@Data
@Builder
public class LoyaltyTierDto {
    private UUID id;
    private String name;
    private int minPoints;
    private String color;
    private String icon;
    private String perks;
    private int sortOrder;
    private boolean active;
    private Instant createdAt;
}

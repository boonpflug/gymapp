package com.gymplatform.modules.loyalty.dto;

import com.gymplatform.modules.loyalty.BadgeCategory;
import com.gymplatform.modules.loyalty.BadgeCriteriaType;
import lombok.Builder;
import lombok.Data;

import java.time.Instant;
import java.util.UUID;

@Data
@Builder
public class LoyaltyBadgeDto {
    private UUID id;
    private String name;
    private String description;
    private String icon;
    private BadgeCategory category;
    private BadgeCriteriaType criteriaType;
    private Integer criteriaValue;
    private boolean active;
    private Instant createdAt;
}

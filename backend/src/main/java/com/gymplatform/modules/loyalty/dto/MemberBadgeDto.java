package com.gymplatform.modules.loyalty.dto;

import com.gymplatform.modules.loyalty.BadgeCategory;
import lombok.Builder;
import lombok.Data;

import java.time.Instant;
import java.util.UUID;

@Data
@Builder
public class MemberBadgeDto {
    private UUID id;
    private UUID memberId;
    private UUID badgeId;
    private String badgeName;
    private String badgeDescription;
    private String badgeIcon;
    private BadgeCategory badgeCategory;
    private Instant earnedAt;
}

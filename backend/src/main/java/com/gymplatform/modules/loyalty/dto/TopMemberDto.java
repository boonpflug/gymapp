package com.gymplatform.modules.loyalty.dto;

import lombok.Builder;
import lombok.Data;

import java.util.UUID;

@Data
@Builder
public class TopMemberDto {
    private UUID memberId;
    private String memberName;
    private int totalPoints;
    private String tierName;
}

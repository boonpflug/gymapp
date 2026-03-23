package com.gymplatform.modules.reporting.dto;

import lombok.Builder;
import lombok.Data;

import java.util.UUID;

@Data
@Builder
public class TopCheckinMemberDto {
    private UUID memberId;
    private String memberName;
    private long checkInCount;
}

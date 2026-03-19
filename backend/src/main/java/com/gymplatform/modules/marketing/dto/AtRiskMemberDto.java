package com.gymplatform.modules.marketing.dto;

import lombok.Builder;
import lombok.Data;

import java.time.Instant;
import java.util.UUID;

@Data
@Builder
public class AtRiskMemberDto {
    private UUID memberId;
    private String firstName;
    private String lastName;
    private String email;
    private String phone;
    private String memberStatus;
    private String contractStatus;
    private Instant lastCheckIn;
    private int daysSinceLastCheckIn;
    private double avgWeeklyVisits;
    private double visitTrend;
    private String riskLevel;
    private String riskReason;
}

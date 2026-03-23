package com.gymplatform.modules.loyalty.dto;

import com.gymplatform.modules.loyalty.ReferralStatus;
import lombok.Builder;
import lombok.Data;

import java.time.Instant;
import java.util.UUID;

@Data
@Builder
public class ReferralDto {
    private UUID id;
    private UUID referrerMemberId;
    private String referrerName;
    private UUID referredMemberId;
    private String referredName;
    private String referredEmail;
    private String referralCode;
    private ReferralStatus status;
    private int referrerPointsAwarded;
    private int referredPointsAwarded;
    private Instant convertedAt;
    private Instant createdAt;
}

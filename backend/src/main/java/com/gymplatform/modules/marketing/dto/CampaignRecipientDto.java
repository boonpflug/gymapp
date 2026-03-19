package com.gymplatform.modules.marketing.dto;

import com.gymplatform.modules.marketing.CampaignEventType;
import lombok.Builder;
import lombok.Data;

import java.time.Instant;
import java.util.UUID;

@Data
@Builder
public class CampaignRecipientDto {
    private UUID id;
    private UUID campaignId;
    private UUID memberId;
    private String memberName;
    private String memberEmail;
    private String recipientAddress;
    private CampaignEventType status;
    private Instant sentAt;
    private Instant deliveredAt;
    private Instant openedAt;
    private Instant clickedAt;
    private String errorMessage;
    private Instant createdAt;
}

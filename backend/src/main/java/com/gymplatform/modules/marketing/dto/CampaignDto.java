package com.gymplatform.modules.marketing.dto;

import com.gymplatform.modules.marketing.CampaignStatus;
import com.gymplatform.modules.marketing.CampaignType;
import lombok.Builder;
import lombok.Data;

import java.time.Instant;
import java.util.UUID;

@Data
@Builder
public class CampaignDto {
    private UUID id;
    private String name;
    private String description;
    private CampaignType campaignType;
    private CampaignStatus status;
    private UUID templateId;
    private String templateName;
    private String subject;
    private String bodyHtml;
    private String bodyText;
    private String audienceCriteria;
    private Instant scheduledAt;
    private Instant sentAt;
    private Integer totalRecipients;
    private Integer sentCount;
    private Integer deliveredCount;
    private Integer openedCount;
    private Integer clickedCount;
    private Integer failedCount;
    private Integer convertedCount;
    private Double deliveryRate;
    private Double openRate;
    private Double clickRate;
    private UUID createdBy;
    private Instant createdAt;
    private Instant updatedAt;
}

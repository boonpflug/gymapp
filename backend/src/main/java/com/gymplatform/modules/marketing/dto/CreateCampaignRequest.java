package com.gymplatform.modules.marketing.dto;

import com.gymplatform.modules.marketing.CampaignType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.Instant;
import java.util.UUID;

@Data
public class CreateCampaignRequest {
    @NotBlank(message = "Campaign name is required")
    private String name;
    private String description;
    @NotNull(message = "Campaign type is required")
    private CampaignType campaignType;
    private UUID templateId;
    private String subject;
    private String bodyHtml;
    private String bodyText;
    private AudienceCriteria audienceCriteria;
    private Instant scheduledAt;
}

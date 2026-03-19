package com.gymplatform.modules.marketing;

import com.gymplatform.shared.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "campaigns")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Campaign extends BaseEntity {

    @Column(nullable = false, length = 200)
    private String name;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(name = "campaign_type", nullable = false, length = 20)
    private CampaignType campaignType;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private CampaignStatus status;

    @Column(name = "template_id")
    private UUID templateId;

    @Column(name = "subject", length = 500)
    private String subject;

    @Column(name = "body_html", columnDefinition = "TEXT")
    private String bodyHtml;

    @Column(name = "body_text", columnDefinition = "TEXT")
    private String bodyText;

    @Column(name = "audience_criteria", columnDefinition = "TEXT")
    private String audienceCriteria;

    @Column(name = "scheduled_at")
    private Instant scheduledAt;

    @Column(name = "sent_at")
    private Instant sentAt;

    @Column(name = "total_recipients")
    private Integer totalRecipients;

    @Column(name = "sent_count")
    private Integer sentCount;

    @Column(name = "delivered_count")
    private Integer deliveredCount;

    @Column(name = "opened_count")
    private Integer openedCount;

    @Column(name = "clicked_count")
    private Integer clickedCount;

    @Column(name = "failed_count")
    private Integer failedCount;

    @Column(name = "converted_count")
    private Integer convertedCount;

    @Column(name = "created_by")
    private UUID createdBy;

    @Column(name = "tenant_id", length = 100)
    private String tenantId;
}

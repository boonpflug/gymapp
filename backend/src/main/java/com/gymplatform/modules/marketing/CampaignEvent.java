package com.gymplatform.modules.marketing;

import com.gymplatform.shared.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

import java.util.UUID;

@Entity
@Table(name = "campaign_events")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class CampaignEvent extends BaseEntity {

    @Column(name = "campaign_id", nullable = false)
    private UUID campaignId;

    @Column(name = "recipient_id")
    private UUID recipientId;

    @Column(name = "member_id")
    private UUID memberId;

    @Enumerated(EnumType.STRING)
    @Column(name = "event_type", nullable = false, length = 20)
    private CampaignEventType eventType;

    @Column(columnDefinition = "TEXT")
    private String metadata;

    @Column(name = "tenant_id", length = 100)
    private String tenantId;
}

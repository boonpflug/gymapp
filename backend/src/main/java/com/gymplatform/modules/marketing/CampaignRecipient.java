package com.gymplatform.modules.marketing;

import com.gymplatform.shared.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "campaign_recipients")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class CampaignRecipient extends BaseEntity {

    @Column(name = "campaign_id", nullable = false)
    private UUID campaignId;

    @Column(name = "member_id", nullable = false)
    private UUID memberId;

    @Column(name = "recipient_address", length = 300)
    private String recipientAddress;

    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    private CampaignEventType status;

    @Column(name = "sent_at")
    private Instant sentAt;

    @Column(name = "delivered_at")
    private Instant deliveredAt;

    @Column(name = "opened_at")
    private Instant openedAt;

    @Column(name = "clicked_at")
    private Instant clickedAt;

    @Column(name = "error_message", length = 1000)
    private String errorMessage;

    @Column(name = "tenant_id", length = 100)
    private String tenantId;
}

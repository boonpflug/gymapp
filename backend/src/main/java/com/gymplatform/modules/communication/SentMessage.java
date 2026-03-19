package com.gymplatform.modules.communication;

import com.gymplatform.shared.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "sent_messages")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class SentMessage extends BaseEntity {

    @Column(name = "member_id", nullable = false)
    private UUID memberId;

    @Column(name = "template_id")
    private UUID templateId;

    @Enumerated(EnumType.STRING)
    @Column(name = "channel_type", nullable = false, length = 20)
    private ChannelType channelType;

    @Column(length = 500)
    private String subject;

    @Column(name = "body_preview", length = 1000)
    private String bodyPreview;

    @Column(name = "recipient_address", length = 300)
    private String recipientAddress;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private MessageStatus status;

    @Column(name = "sent_at")
    private Instant sentAt;

    @Column(name = "delivered_at")
    private Instant deliveredAt;

    @Column(name = "failed_at")
    private Instant failedAt;

    @Column(name = "opened_at")
    private Instant openedAt;

    @Column(name = "error_message", length = 1000)
    private String errorMessage;

    @Column(name = "trigger_event", length = 40)
    private String triggerEvent;

    @Column(name = "sent_by")
    private UUID sentBy;

    @Column(name = "tenant_id", length = 100)
    private String tenantId;
}

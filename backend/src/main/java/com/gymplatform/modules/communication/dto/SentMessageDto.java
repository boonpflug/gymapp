package com.gymplatform.modules.communication.dto;

import com.gymplatform.modules.communication.ChannelType;
import com.gymplatform.modules.communication.MessageStatus;
import lombok.Builder;
import lombok.Data;

import java.time.Instant;
import java.util.UUID;

@Data
@Builder
public class SentMessageDto {
    private UUID id;
    private UUID memberId;
    private String memberName;
    private UUID templateId;
    private String templateName;
    private ChannelType channelType;
    private String subject;
    private String bodyPreview;
    private String recipientAddress;
    private MessageStatus status;
    private Instant sentAt;
    private Instant deliveredAt;
    private Instant failedAt;
    private Instant openedAt;
    private String errorMessage;
    private String triggerEvent;
    private Instant createdAt;
}

package com.gymplatform.modules.communication.dto;

import com.gymplatform.modules.communication.ChannelType;
import lombok.Builder;
import lombok.Data;

import java.time.Instant;
import java.util.UUID;

@Data
@Builder
public class CommunicationTemplateDto {
    private UUID id;
    private String name;
    private ChannelType channelType;
    private String subject;
    private String bodyHtml;
    private String bodyText;
    private String category;
    private String locale;
    private String logoUrl;
    private String brandColor;
    private boolean active;
    private Instant createdAt;
    private Instant updatedAt;
}

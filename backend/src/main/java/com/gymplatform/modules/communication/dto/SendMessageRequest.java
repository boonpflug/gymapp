package com.gymplatform.modules.communication.dto;

import com.gymplatform.modules.communication.ChannelType;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.Map;
import java.util.UUID;

@Data
public class SendMessageRequest {
    @NotNull(message = "Member ID is required")
    private UUID memberId;
    @NotNull(message = "Template ID is required")
    private UUID templateId;
    @NotNull(message = "Channel type is required")
    private ChannelType channelType;
    private Map<String, String> variables;
}

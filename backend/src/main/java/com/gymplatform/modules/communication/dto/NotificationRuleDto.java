package com.gymplatform.modules.communication.dto;

import com.gymplatform.modules.communication.ChannelType;
import com.gymplatform.modules.communication.DelayDirection;
import com.gymplatform.modules.communication.TriggerEvent;
import lombok.Builder;
import lombok.Data;

import java.time.Instant;
import java.util.UUID;

@Data
@Builder
public class NotificationRuleDto {
    private UUID id;
    private String name;
    private TriggerEvent triggerEvent;
    private UUID templateId;
    private String templateName;
    private ChannelType channelType;
    private int delayDays;
    private DelayDirection delayDirection;
    private String description;
    private boolean active;
    private Instant createdAt;
}

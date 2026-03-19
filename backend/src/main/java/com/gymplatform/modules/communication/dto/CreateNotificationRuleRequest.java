package com.gymplatform.modules.communication.dto;

import com.gymplatform.modules.communication.ChannelType;
import com.gymplatform.modules.communication.DelayDirection;
import com.gymplatform.modules.communication.TriggerEvent;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.UUID;

@Data
public class CreateNotificationRuleRequest {
    @NotBlank(message = "Rule name is required")
    private String name;
    @NotNull(message = "Trigger event is required")
    private TriggerEvent triggerEvent;
    @NotNull(message = "Template ID is required")
    private UUID templateId;
    @NotNull(message = "Channel type is required")
    private ChannelType channelType;
    private int delayDays;
    @NotNull(message = "Delay direction is required")
    private DelayDirection delayDirection;
    private String description;
}

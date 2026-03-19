package com.gymplatform.modules.communication;

import com.gymplatform.shared.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

import java.util.UUID;

@Entity
@Table(name = "notification_rules")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class NotificationRule extends BaseEntity {

    @Column(nullable = false, length = 200)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(name = "trigger_event", nullable = false, length = 40)
    private TriggerEvent triggerEvent;

    @Column(name = "template_id", nullable = false)
    private UUID templateId;

    @Enumerated(EnumType.STRING)
    @Column(name = "channel_type", nullable = false, length = 20)
    private ChannelType channelType;

    @Column(name = "delay_days")
    private int delayDays;

    @Enumerated(EnumType.STRING)
    @Column(name = "delay_direction", nullable = false, length = 20)
    private DelayDirection delayDirection;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(nullable = false)
    private boolean active = true;

    @Column(name = "tenant_id", length = 100)
    private String tenantId;
}

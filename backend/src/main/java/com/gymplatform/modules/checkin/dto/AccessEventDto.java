package com.gymplatform.modules.checkin.dto;

import com.gymplatform.modules.checkin.AccessEventType;
import lombok.Builder;
import lombok.Data;

import java.time.Instant;
import java.util.UUID;

@Data
@Builder
public class AccessEventDto {
    private UUID id;
    private UUID deviceId;
    private String deviceName;
    private UUID memberId;
    private String memberName;
    private AccessEventType eventType;
    private String reasonCode;
    private String details;
    private Instant createdAt;
}

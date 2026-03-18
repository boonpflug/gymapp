package com.gymplatform.modules.checkin;

import lombok.Builder;
import lombok.Data;

import java.time.Instant;

@Data
@Builder
public class DeviceStatus {
    private boolean online;
    private String firmwareVersion;
    private Instant lastHeartbeat;
    private String errorMessage;
}

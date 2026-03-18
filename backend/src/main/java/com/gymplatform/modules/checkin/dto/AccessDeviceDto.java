package com.gymplatform.modules.checkin.dto;

import com.gymplatform.modules.checkin.DeviceMode;
import com.gymplatform.modules.checkin.DeviceType;
import lombok.Builder;
import lombok.Data;

import java.time.Instant;
import java.util.UUID;

@Data
@Builder
public class AccessDeviceDto {
    private UUID id;
    private String name;
    private DeviceType deviceType;
    private DeviceMode mode;
    private String locationDescription;
    private String ipAddress;
    private String apiEndpoint;
    private Integer maxOccupancy;
    private boolean active;
    private Instant lastHeartbeatAt;
}

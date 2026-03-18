package com.gymplatform.modules.checkin.dto;

import com.gymplatform.modules.checkin.CheckInMethod;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.UUID;

@Data
public class DeviceCheckInRequest {

    @NotNull(message = "Member ID is required")
    private UUID memberId;

    @NotNull(message = "Device ID is required")
    private UUID deviceId;

    @NotNull(message = "Check-in method is required")
    private CheckInMethod method;
}

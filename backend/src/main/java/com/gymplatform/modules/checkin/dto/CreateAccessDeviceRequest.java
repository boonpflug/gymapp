package com.gymplatform.modules.checkin.dto;

import com.gymplatform.modules.checkin.DeviceMode;
import com.gymplatform.modules.checkin.DeviceType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class CreateAccessDeviceRequest {

    @NotBlank(message = "Device name is required")
    private String name;

    @NotNull(message = "Device type is required")
    private DeviceType deviceType;

    private DeviceMode mode = DeviceMode.QR;
    private String locationDescription;
    private String ipAddress;
    private String apiEndpoint;
    private String apiKey;
    private Integer maxOccupancy;
}

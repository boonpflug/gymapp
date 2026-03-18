package com.gymplatform.modules.checkin.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.UUID;

@Data
public class ManualCheckInRequest {

    @NotNull(message = "Member ID is required")
    private UUID memberId;

    private UUID deviceId;
}

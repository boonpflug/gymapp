package com.gymplatform.modules.training.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.UUID;

@Data
public class StartSessionRequest {
    @NotNull(message = "Member ID is required")
    private UUID memberId;
    private UUID planId;
}

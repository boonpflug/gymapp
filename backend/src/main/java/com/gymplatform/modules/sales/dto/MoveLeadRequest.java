package com.gymplatform.modules.sales.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.UUID;

@Data
public class MoveLeadRequest {
    @NotNull(message = "Stage ID is required")
    private UUID stageId;
}

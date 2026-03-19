package com.gymplatform.modules.sales.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class CreateLeadStageRequest {
    @NotBlank(message = "Stage name is required")
    private String name;
    private int sortOrder;
    private String color;
    private boolean isDefault;
    private boolean isClosed;
    private boolean isWon;
}

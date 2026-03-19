package com.gymplatform.modules.sales.dto;

import lombok.Builder;
import lombok.Data;

import java.util.UUID;

@Data
@Builder
public class LeadStageDto {
    private UUID id;
    private String name;
    private int sortOrder;
    private String color;
    private boolean isDefault;
    private boolean isClosed;
    private boolean isWon;
    private long leadCount;
}

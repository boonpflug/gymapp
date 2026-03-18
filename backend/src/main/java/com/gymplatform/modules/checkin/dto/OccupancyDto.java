package com.gymplatform.modules.checkin.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class OccupancyDto {
    private long currentCount;
    private Integer maxCapacity;
    private boolean atCapacity;
}

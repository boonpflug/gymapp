package com.gymplatform.modules.communication.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class MessageStatsDto {
    private long totalSent;
    private long delivered;
    private long failed;
    private long opened;
    private long pending;
}

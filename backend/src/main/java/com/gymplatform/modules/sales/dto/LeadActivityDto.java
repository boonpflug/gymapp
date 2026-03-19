package com.gymplatform.modules.sales.dto;

import com.gymplatform.modules.sales.LeadActivityType;
import lombok.Builder;
import lombok.Data;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

@Data
@Builder
public class LeadActivityDto {
    private UUID id;
    private UUID leadId;
    private LeadActivityType activityType;
    private String description;
    private String outcome;
    private UUID staffId;
    private String staffName;
    private LocalDate dueDate;
    private Instant completedAt;
    private Instant createdAt;
}

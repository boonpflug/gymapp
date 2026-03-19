package com.gymplatform.modules.sales.dto;

import com.gymplatform.modules.sales.LeadActivityType;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDate;
import java.util.UUID;

@Data
public class CreateLeadActivityRequest {
    @NotNull(message = "Lead ID is required")
    private UUID leadId;
    @NotNull(message = "Activity type is required")
    private LeadActivityType activityType;
    private String description;
    private String outcome;
    private LocalDate dueDate;
}

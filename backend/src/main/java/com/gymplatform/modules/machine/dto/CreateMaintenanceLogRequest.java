package com.gymplatform.modules.machine.dto;

import com.gymplatform.modules.machine.MaintenanceType;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

@Data
public class CreateMaintenanceLogRequest {
    @NotNull
    private UUID machineId;
    @NotNull
    private MaintenanceType maintenanceType;
    private String description;
    private String performedBy;
    @NotNull
    private Instant performedAt;
    private LocalDate nextDueDate;
    private BigDecimal cost;
    private String notes;
}

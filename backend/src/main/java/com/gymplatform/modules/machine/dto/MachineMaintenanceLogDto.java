package com.gymplatform.modules.machine.dto;

import com.gymplatform.modules.machine.MaintenanceType;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

@Data
@Builder
public class MachineMaintenanceLogDto {
    private UUID id;
    private UUID machineId;
    private String machineName;
    private MaintenanceType maintenanceType;
    private String description;
    private String performedBy;
    private Instant performedAt;
    private LocalDate nextDueDate;
    private BigDecimal cost;
    private String notes;
    private Instant createdAt;
}

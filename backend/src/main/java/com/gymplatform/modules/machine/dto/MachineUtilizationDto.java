package com.gymplatform.modules.machine.dto;

import lombok.Builder;
import lombok.Data;

import java.time.Instant;
import java.util.UUID;

@Data
@Builder
public class MachineUtilizationDto {
    private UUID machineId;
    private String machineCode;
    private String machineName;
    private long totalSessions;
    private long totalMembers;
    private double avgSessionDurationSeconds;
    private Instant lastUsedAt;
}

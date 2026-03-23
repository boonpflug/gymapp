package com.gymplatform.modules.machine.dto;

import com.gymplatform.modules.machine.MachineStatus;
import lombok.Builder;
import lombok.Data;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

@Data
@Builder
public class MachineDto {
    private UUID id;
    private String code;
    private String name;
    private String fullName;
    private String series;
    private String category;
    private UUID facilityId;
    private String serialNumber;
    private String model;
    private String firmwareVersion;
    private LocalDate installationDate;
    private boolean isComputerAssisted;
    private MachineStatus status;
    private String imageUrl;
    private String notes;
    private LocalDate lastMaintenanceDate;
    private LocalDate nextMaintenanceDate;
    private Instant createdAt;
}

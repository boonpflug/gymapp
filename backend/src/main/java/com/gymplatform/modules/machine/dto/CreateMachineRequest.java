package com.gymplatform.modules.machine.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.time.LocalDate;
import java.util.UUID;

@Data
public class CreateMachineRequest {
    @NotBlank
    private String code;
    @NotBlank
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
    private String imageUrl;
    private String notes;
}

package com.gymplatform.modules.machine;

import com.gymplatform.shared.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "machines")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Machine extends BaseEntity {

    @Column(nullable = false, length = 20)
    private String code;

    @Column(nullable = false)
    private String name;

    @Column(name = "full_name")
    private String fullName;

    private String series;

    private String category;

    @Column(name = "facility_id")
    private UUID facilityId;

    @Column(name = "serial_number")
    private String serialNumber;

    private String model;

    @Column(name = "firmware_version")
    private String firmwareVersion;

    @Column(name = "installation_date")
    private LocalDate installationDate;

    @Column(name = "is_computer_assisted")
    private boolean isComputerAssisted;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private MachineStatus status;

    @Column(name = "image_url")
    private String imageUrl;

    @Column(columnDefinition = "TEXT")
    private String notes;

    @Column(name = "last_maintenance_date")
    private LocalDate lastMaintenanceDate;

    @Column(name = "next_maintenance_date")
    private LocalDate nextMaintenanceDate;

    @Column(name = "tenant_id", length = 100)
    private String tenantId;
}

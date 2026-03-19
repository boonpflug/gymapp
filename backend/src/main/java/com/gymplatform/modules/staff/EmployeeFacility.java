package com.gymplatform.modules.staff;

import com.gymplatform.shared.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

import java.util.UUID;

@Entity
@Table(name = "employee_facilities")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class EmployeeFacility extends BaseEntity {

    @Column(name = "employee_id", nullable = false)
    private UUID employeeId;

    @Column(name = "facility_id", nullable = false)
    private UUID facilityId;

    @Column(name = "is_primary")
    private boolean primary = false;

    @Column(name = "tenant_id", length = 100)
    private String tenantId;
}

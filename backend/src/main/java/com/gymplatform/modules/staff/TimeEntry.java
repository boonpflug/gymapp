package com.gymplatform.modules.staff;

import com.gymplatform.shared.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "time_entries")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class TimeEntry extends BaseEntity {

    @Column(name = "employee_id", nullable = false)
    private UUID employeeId;

    @Column(name = "shift_id")
    private UUID shiftId;

    @Column(name = "clock_in", nullable = false)
    private Instant clockIn;

    @Column(name = "clock_out")
    private Instant clockOut;

    @Column(name = "break_minutes")
    private Integer breakMinutes;

    @Column(name = "total_minutes")
    private Integer totalMinutes;

    @Column(columnDefinition = "TEXT")
    private String notes;

    @Column(name = "tenant_id", length = 100)
    private String tenantId;
}

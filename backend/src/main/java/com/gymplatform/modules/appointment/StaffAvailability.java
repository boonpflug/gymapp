package com.gymplatform.modules.appointment;

import com.gymplatform.shared.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.UUID;

@Entity
@Table(name = "staff_availability")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class StaffAvailability extends BaseEntity {

    @Column(name = "staff_id", nullable = false)
    private UUID staffId;

    @Column(name = "facility_id", nullable = false)
    private UUID facilityId;

    @Column(name = "day_of_week")
    private int dayOfWeek;

    @Column(name = "start_time", nullable = false)
    private LocalTime startTime;

    @Column(name = "end_time", nullable = false)
    private LocalTime endTime;

    @Column(nullable = false)
    private boolean recurring;

    @Column(name = "specific_date")
    private LocalDate specificDate;

    @Column(nullable = false)
    private boolean available;

    @Column(name = "tenant_id", length = 100)
    private String tenantId;
}

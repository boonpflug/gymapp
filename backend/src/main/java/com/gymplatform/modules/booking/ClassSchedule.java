package com.gymplatform.modules.booking;

import com.gymplatform.shared.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "class_schedules")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ClassSchedule extends BaseEntity {

    @Column(name = "class_id", nullable = false)
    private UUID classId;

    @Column(name = "trainer_id")
    private UUID trainerId;

    @Column(name = "start_time", nullable = false)
    private Instant startTime;

    @Column(name = "end_time", nullable = false)
    private Instant endTime;

    @Column(length = 100)
    private String room;

    @Column(name = "capacity_override")
    private Integer capacityOverride;

    @Column(name = "virtual_link", length = 500)
    private String virtualLink;

    @Column(nullable = false)
    private boolean cancelled = false;

    @Column(name = "cancellation_reason", columnDefinition = "TEXT")
    private String cancellationReason;

    @Enumerated(EnumType.STRING)
    @Column(name = "recurrence_rule", length = 20)
    private RecurrenceRule recurrenceRule;

    @Column(name = "recurrence_group_id")
    private UUID recurrenceGroupId;

    @Column(name = "tenant_id", length = 100)
    private String tenantId;
}

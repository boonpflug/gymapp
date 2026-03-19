package com.gymplatform.modules.booking;

import com.gymplatform.shared.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

import java.util.UUID;

@Entity
@Table(name = "classes")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ClassDefinition extends BaseEntity {

    @Column(nullable = false, length = 200)
    private String name;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(name = "category_id")
    private UUID categoryId;

    @Column(name = "trainer_id")
    private UUID trainerId;

    @Column(length = 100)
    private String room;

    @Column(nullable = false)
    private int capacity;

    @Column(name = "duration_minutes", nullable = false)
    private int durationMinutes;

    @Column(name = "virtual_link", length = 500)
    private String virtualLink;

    @Column(name = "allow_waitlist")
    private boolean allowWaitlist = true;

    @Column(name = "booking_cutoff_minutes")
    private int bookingCutoffMinutes = 60;

    @Column(name = "cancellation_cutoff_minutes")
    private int cancellationCutoffMinutes = 120;

    @Column(name = "allow_trial")
    private boolean allowTrial = false;

    @Column(nullable = false)
    private boolean active = true;

    @Column(name = "tenant_id", length = 100)
    private String tenantId;
}

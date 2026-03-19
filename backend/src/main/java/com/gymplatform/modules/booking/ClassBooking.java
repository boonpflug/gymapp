package com.gymplatform.modules.booking;

import com.gymplatform.shared.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "class_bookings")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ClassBooking extends BaseEntity {

    @Column(name = "schedule_id", nullable = false)
    private UUID scheduleId;

    @Column(name = "member_id")
    private UUID memberId;

    @Column(name = "guest_name", length = 200)
    private String guestName;

    @Column(name = "guest_email", length = 200)
    private String guestEmail;

    @Column(name = "guest_phone", length = 50)
    private String guestPhone;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private BookingStatus status;

    @Column(name = "booked_at", nullable = false)
    private Instant bookedAt;

    @Column(name = "cancelled_at")
    private Instant cancelledAt;

    @Column(name = "attendance_marked_at")
    private Instant attendanceMarkedAt;

    @Column(name = "attendance_marked_by")
    private UUID attendanceMarkedBy;

    @Column(name = "tenant_id", length = 100)
    private String tenantId;
}

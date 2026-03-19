package com.gymplatform.modules.booking.dto;

import com.gymplatform.modules.booking.BookingStatus;
import lombok.Builder;
import lombok.Data;

import java.time.Instant;
import java.util.UUID;

@Data
@Builder
public class ClassBookingDto {
    private UUID id;
    private UUID scheduleId;
    private String className;
    private Instant classStartTime;
    private UUID memberId;
    private String memberName;
    private String guestName;
    private String guestEmail;
    private String guestPhone;
    private BookingStatus status;
    private Instant bookedAt;
    private Instant cancelledAt;
    private Instant attendanceMarkedAt;
}

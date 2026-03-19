package com.gymplatform.modules.booking.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.UUID;

@Data
public class CreateBookingRequest {
    @NotNull(message = "Schedule ID is required")
    private UUID scheduleId;

    private UUID memberId;

    // For trial/guest bookings
    private String guestName;
    private String guestEmail;
    private String guestPhone;
}

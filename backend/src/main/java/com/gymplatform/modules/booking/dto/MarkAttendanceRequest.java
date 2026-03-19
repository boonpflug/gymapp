package com.gymplatform.modules.booking.dto;

import com.gymplatform.modules.booking.BookingStatus;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.UUID;

@Data
public class MarkAttendanceRequest {
    @NotNull(message = "Booking ID is required")
    private UUID bookingId;

    @NotNull(message = "Status is required (ATTENDED or NO_SHOW)")
    private BookingStatus status;
}

package com.gymplatform.modules.booking;

import com.gymplatform.modules.booking.dto.*;
import com.gymplatform.shared.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/booking/bookings")
@RequiredArgsConstructor
public class ClassBookingController {

    private final ClassBookingService bookingService;

    @PostMapping
    @PreAuthorize("hasAnyRole('STUDIO_OWNER', 'MANAGER', 'RECEPTIONIST', 'MEMBER')")
    public ResponseEntity<ApiResponse<ClassBookingDto>> book(
            @Valid @RequestBody CreateBookingRequest req,
            @AuthenticationPrincipal UserDetails user) {
        UUID userId = UUID.fromString(user.getUsername());
        return ResponseEntity.ok(ApiResponse.success(bookingService.book(req, userId)));
    }

    @PostMapping("/{id}/cancel")
    @PreAuthorize("hasAnyRole('STUDIO_OWNER', 'MANAGER', 'RECEPTIONIST', 'MEMBER')")
    public ResponseEntity<ApiResponse<ClassBookingDto>> cancel(
            @PathVariable UUID id,
            @AuthenticationPrincipal UserDetails user) {
        UUID userId = UUID.fromString(user.getUsername());
        return ResponseEntity.ok(ApiResponse.success(bookingService.cancel(id, userId)));
    }

    @PostMapping("/attendance")
    @PreAuthorize("hasAnyRole('STUDIO_OWNER', 'MANAGER', 'TRAINER', 'RECEPTIONIST')")
    public ResponseEntity<ApiResponse<ClassBookingDto>> markAttendance(
            @Valid @RequestBody MarkAttendanceRequest req,
            @AuthenticationPrincipal UserDetails user) {
        UUID staffId = UUID.fromString(user.getUsername());
        return ResponseEntity.ok(ApiResponse.success(
                bookingService.markAttendance(req.getBookingId(), req.getStatus(), staffId)));
    }

    @GetMapping("/member/{memberId}")
    @PreAuthorize("hasAnyRole('STUDIO_OWNER', 'MANAGER', 'TRAINER', 'RECEPTIONIST', 'MEMBER')")
    public ResponseEntity<ApiResponse<Page<ClassBookingDto>>> getMemberBookings(
            @PathVariable UUID memberId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(ApiResponse.success(
                bookingService.getMemberBookings(memberId, PageRequest.of(page, size))));
    }

    @GetMapping("/schedule/{scheduleId}")
    @PreAuthorize("hasAnyRole('STUDIO_OWNER', 'MANAGER', 'TRAINER', 'RECEPTIONIST')")
    public ResponseEntity<ApiResponse<List<ClassBookingDto>>> getScheduleBookings(
            @PathVariable UUID scheduleId) {
        return ResponseEntity.ok(ApiResponse.success(bookingService.getScheduleBookings(scheduleId)));
    }

    @GetMapping("/schedule/{scheduleId}/waitlist")
    @PreAuthorize("hasAnyRole('STUDIO_OWNER', 'MANAGER', 'TRAINER', 'RECEPTIONIST')")
    public ResponseEntity<ApiResponse<List<WaitlistEntryDto>>> getScheduleWaitlist(
            @PathVariable UUID scheduleId) {
        return ResponseEntity.ok(ApiResponse.success(bookingService.getScheduleWaitlist(scheduleId)));
    }

    @PostMapping("/waitlist/{entryId}/cancel")
    @PreAuthorize("hasAnyRole('STUDIO_OWNER', 'MANAGER', 'RECEPTIONIST', 'MEMBER')")
    public ResponseEntity<ApiResponse<Void>> cancelWaitlistEntry(
            @PathVariable UUID entryId,
            @AuthenticationPrincipal UserDetails user) {
        UUID userId = UUID.fromString(user.getUsername());
        bookingService.cancelWaitlistEntry(entryId, userId);
        return ResponseEntity.ok(ApiResponse.success(null));
    }
}

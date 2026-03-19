package com.gymplatform.modules.staff;

import com.gymplatform.modules.staff.dto.ClockInRequest;
import com.gymplatform.modules.staff.dto.ClockOutRequest;
import com.gymplatform.modules.staff.dto.TimeEntryDto;
import com.gymplatform.shared.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/staff/time")
@RequiredArgsConstructor
public class TimeTrackingController {

    private final TimeTrackingService timeTrackingService;

    @PostMapping("/clock-in")
    @PreAuthorize("hasAnyRole('STUDIO_OWNER', 'MANAGER', 'TRAINER', 'RECEPTIONIST')")
    public ResponseEntity<ApiResponse<TimeEntryDto>> clockIn(
            @Valid @RequestBody ClockInRequest req,
            @AuthenticationPrincipal UserDetails user) {
        UUID userId = UUID.fromString(user.getUsername());
        return ResponseEntity.ok(ApiResponse.success(timeTrackingService.clockIn(req, userId)));
    }

    @PostMapping("/{id}/clock-out")
    @PreAuthorize("hasAnyRole('STUDIO_OWNER', 'MANAGER', 'TRAINER', 'RECEPTIONIST')")
    public ResponseEntity<ApiResponse<TimeEntryDto>> clockOut(
            @PathVariable UUID id,
            @RequestBody ClockOutRequest req,
            @AuthenticationPrincipal UserDetails user) {
        UUID userId = UUID.fromString(user.getUsername());
        return ResponseEntity.ok(ApiResponse.success(timeTrackingService.clockOut(id, req, userId)));
    }

    @GetMapping("/active/{employeeId}")
    @PreAuthorize("hasAnyRole('STUDIO_OWNER', 'MANAGER', 'TRAINER', 'RECEPTIONIST')")
    public ResponseEntity<ApiResponse<TimeEntryDto>> getActive(@PathVariable UUID employeeId) {
        return ResponseEntity.ok(ApiResponse.success(timeTrackingService.getActiveEntry(employeeId)));
    }

    @GetMapping("/employee/{employeeId}")
    @PreAuthorize("hasAnyRole('STUDIO_OWNER', 'MANAGER')")
    public ResponseEntity<ApiResponse<List<TimeEntryDto>>> getByEmployee(
            @PathVariable UUID employeeId,
            @RequestParam Instant start,
            @RequestParam Instant end) {
        return ResponseEntity.ok(ApiResponse.success(
                timeTrackingService.getByEmployee(employeeId, start, end)));
    }

    @GetMapping("/employee/{employeeId}/total")
    @PreAuthorize("hasAnyRole('STUDIO_OWNER', 'MANAGER')")
    public ResponseEntity<ApiResponse<Integer>> getTotalMinutes(
            @PathVariable UUID employeeId,
            @RequestParam Instant start,
            @RequestParam Instant end) {
        return ResponseEntity.ok(ApiResponse.success(
                timeTrackingService.getTotalMinutes(employeeId, start, end)));
    }
}

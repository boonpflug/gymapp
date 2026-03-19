package com.gymplatform.modules.staff;

import com.gymplatform.modules.staff.dto.CreateShiftRequest;
import com.gymplatform.modules.staff.dto.ShiftDto;
import com.gymplatform.modules.staff.dto.ShiftReportDto;
import com.gymplatform.shared.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/staff/shifts")
@RequiredArgsConstructor
public class ShiftController {

    private final ShiftService shiftService;

    @PostMapping
    @PreAuthorize("hasAnyRole('STUDIO_OWNER', 'MANAGER')")
    public ResponseEntity<ApiResponse<ShiftDto>> create(
            @Valid @RequestBody CreateShiftRequest req,
            @AuthenticationPrincipal UserDetails user) {
        UUID userId = UUID.fromString(user.getUsername());
        return ResponseEntity.ok(ApiResponse.success(shiftService.create(req, userId)));
    }

    @PostMapping("/{id}/status")
    @PreAuthorize("hasAnyRole('STUDIO_OWNER', 'MANAGER')")
    public ResponseEntity<ApiResponse<ShiftDto>> updateStatus(
            @PathVariable UUID id,
            @RequestParam ShiftStatus status,
            @AuthenticationPrincipal UserDetails user) {
        UUID userId = UUID.fromString(user.getUsername());
        return ResponseEntity.ok(ApiResponse.success(shiftService.updateStatus(id, status, userId)));
    }

    @PostMapping("/{id}/cancel")
    @PreAuthorize("hasAnyRole('STUDIO_OWNER', 'MANAGER')")
    public ResponseEntity<ApiResponse<Void>> cancel(
            @PathVariable UUID id,
            @AuthenticationPrincipal UserDetails user) {
        UUID userId = UUID.fromString(user.getUsername());
        shiftService.cancel(id, userId);
        return ResponseEntity.ok(ApiResponse.success(null));
    }

    @GetMapping("/employee/{employeeId}")
    @PreAuthorize("hasAnyRole('STUDIO_OWNER', 'MANAGER', 'TRAINER', 'RECEPTIONIST')")
    public ResponseEntity<ApiResponse<List<ShiftDto>>> getByEmployee(
            @PathVariable UUID employeeId,
            @RequestParam Instant start,
            @RequestParam Instant end) {
        return ResponseEntity.ok(ApiResponse.success(shiftService.getByEmployee(employeeId, start, end)));
    }

    @GetMapping("/employee/{employeeId}/upcoming")
    @PreAuthorize("hasAnyRole('STUDIO_OWNER', 'MANAGER', 'TRAINER', 'RECEPTIONIST')")
    public ResponseEntity<ApiResponse<List<ShiftDto>>> getUpcoming(@PathVariable UUID employeeId) {
        return ResponseEntity.ok(ApiResponse.success(shiftService.getUpcoming(employeeId)));
    }

    @GetMapping("/weekly")
    @PreAuthorize("hasAnyRole('STUDIO_OWNER', 'MANAGER')")
    public ResponseEntity<ApiResponse<List<ShiftDto>>> getWeekly(
            @RequestParam(required = false) Instant weekStart) {
        Instant start = weekStart != null ? weekStart : Instant.now().truncatedTo(ChronoUnit.DAYS);
        return ResponseEntity.ok(ApiResponse.success(shiftService.getWeeklySchedule(start)));
    }

    @GetMapping("/report")
    @PreAuthorize("hasAnyRole('STUDIO_OWNER', 'MANAGER')")
    public ResponseEntity<ApiResponse<List<ShiftReportDto>>> getReport(
            @RequestParam Instant start,
            @RequestParam Instant end) {
        return ResponseEntity.ok(ApiResponse.success(shiftService.getShiftVsActualReport(start, end)));
    }
}

package com.gymplatform.modules.appointment;

import com.gymplatform.modules.appointment.dto.*;
import com.gymplatform.shared.ApiResponse;
import com.gymplatform.shared.PageMeta;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/appointments")
@RequiredArgsConstructor
public class AppointmentController {

    private final AppointmentService appointmentService;

    // ── Appointment Types ───────────────────────────────────────────────

    @GetMapping("/types")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<List<AppointmentTypeDto>>> listTypes() {
        return ResponseEntity.ok(ApiResponse.success(appointmentService.listTypes()));
    }

    @PostMapping("/types")
    @PreAuthorize("hasAnyRole('STUDIO_OWNER', 'MANAGER')")
    public ResponseEntity<ApiResponse<AppointmentTypeDto>> createType(
            @Valid @RequestBody CreateAppointmentTypeRequest req,
            @AuthenticationPrincipal UserDetails user) {
        UUID userId = UUID.fromString(user.getUsername());
        return ResponseEntity.ok(ApiResponse.success(appointmentService.createType(req, userId)));
    }

    @PutMapping("/types/{id}")
    @PreAuthorize("hasAnyRole('STUDIO_OWNER', 'MANAGER')")
    public ResponseEntity<ApiResponse<AppointmentTypeDto>> updateType(
            @PathVariable UUID id,
            @Valid @RequestBody CreateAppointmentTypeRequest req,
            @AuthenticationPrincipal UserDetails user) {
        UUID userId = UUID.fromString(user.getUsername());
        return ResponseEntity.ok(ApiResponse.success(appointmentService.updateType(id, req, userId)));
    }

    @DeleteMapping("/types/{id}")
    @PreAuthorize("hasAnyRole('STUDIO_OWNER', 'MANAGER')")
    public ResponseEntity<ApiResponse<Void>> deactivateType(
            @PathVariable UUID id,
            @AuthenticationPrincipal UserDetails user) {
        UUID userId = UUID.fromString(user.getUsername());
        appointmentService.deactivateType(id, userId);
        return ResponseEntity.ok(ApiResponse.success(null));
    }

    // ── Staff Availability ──────────────────────────────────────────────

    @PostMapping("/availability")
    @PreAuthorize("hasAnyRole('STUDIO_OWNER', 'MANAGER', 'TRAINER')")
    public ResponseEntity<ApiResponse<StaffAvailabilityDto>> setAvailability(
            @Valid @RequestBody CreateAvailabilityRequest req,
            @AuthenticationPrincipal UserDetails user) {
        UUID userId = UUID.fromString(user.getUsername());
        return ResponseEntity.ok(ApiResponse.success(appointmentService.setAvailability(req, userId)));
    }

    @GetMapping("/availability/{staffId}")
    @PreAuthorize("hasAnyRole('STUDIO_OWNER', 'MANAGER', 'TRAINER')")
    public ResponseEntity<ApiResponse<List<StaffAvailabilityDto>>> getStaffAvailability(
            @PathVariable UUID staffId) {
        return ResponseEntity.ok(ApiResponse.success(appointmentService.getStaffAvailability(staffId)));
    }

    @DeleteMapping("/availability/{id}")
    @PreAuthorize("hasAnyRole('STUDIO_OWNER', 'MANAGER', 'TRAINER')")
    public ResponseEntity<ApiResponse<Void>> deleteAvailability(
            @PathVariable UUID id,
            @AuthenticationPrincipal UserDetails user) {
        UUID userId = UUID.fromString(user.getUsername());
        appointmentService.deleteAvailability(id, userId);
        return ResponseEntity.ok(ApiResponse.success(null));
    }

    // ── Appointments ────────────────────────────────────────────────────

    @PostMapping
    @PreAuthorize("hasAnyRole('STUDIO_OWNER', 'MANAGER', 'RECEPTIONIST', 'MEMBER')")
    public ResponseEntity<ApiResponse<AppointmentDto>> createAppointment(
            @Valid @RequestBody CreateAppointmentRequest req,
            @AuthenticationPrincipal UserDetails user) {
        UUID userId = UUID.fromString(user.getUsername());
        return ResponseEntity.ok(ApiResponse.success(appointmentService.createAppointment(req, userId)));
    }

    @GetMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<AppointmentDto>> getAppointment(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.success(appointmentService.getAppointment(id)));
    }

    @PostMapping("/{id}/confirm")
    @PreAuthorize("hasAnyRole('STUDIO_OWNER', 'MANAGER', 'TRAINER')")
    public ResponseEntity<ApiResponse<AppointmentDto>> confirmAppointment(
            @PathVariable UUID id,
            @AuthenticationPrincipal UserDetails user) {
        UUID userId = UUID.fromString(user.getUsername());
        return ResponseEntity.ok(ApiResponse.success(appointmentService.confirmAppointment(id, userId)));
    }

    @PostMapping("/{id}/start")
    @PreAuthorize("hasAnyRole('STUDIO_OWNER', 'MANAGER', 'TRAINER')")
    public ResponseEntity<ApiResponse<AppointmentDto>> startAppointment(
            @PathVariable UUID id,
            @AuthenticationPrincipal UserDetails user) {
        UUID userId = UUID.fromString(user.getUsername());
        return ResponseEntity.ok(ApiResponse.success(appointmentService.startAppointment(id, userId)));
    }

    @PostMapping("/{id}/complete")
    @PreAuthorize("hasAnyRole('STUDIO_OWNER', 'MANAGER', 'TRAINER')")
    public ResponseEntity<ApiResponse<AppointmentDto>> completeAppointment(
            @PathVariable UUID id,
            @AuthenticationPrincipal UserDetails user) {
        UUID userId = UUID.fromString(user.getUsername());
        return ResponseEntity.ok(ApiResponse.success(appointmentService.completeAppointment(id, userId)));
    }

    @PostMapping("/{id}/cancel")
    @PreAuthorize("hasAnyRole('STUDIO_OWNER', 'MANAGER', 'TRAINER', 'MEMBER')")
    public ResponseEntity<ApiResponse<AppointmentDto>> cancelAppointment(
            @PathVariable UUID id,
            @RequestParam String reason,
            @AuthenticationPrincipal UserDetails user) {
        UUID userId = UUID.fromString(user.getUsername());
        return ResponseEntity.ok(ApiResponse.success(appointmentService.cancelAppointment(id, reason, userId)));
    }

    @PostMapping("/{id}/no-show")
    @PreAuthorize("hasAnyRole('STUDIO_OWNER', 'MANAGER', 'TRAINER')")
    public ResponseEntity<ApiResponse<AppointmentDto>> markNoShow(
            @PathVariable UUID id,
            @AuthenticationPrincipal UserDetails user) {
        UUID userId = UUID.fromString(user.getUsername());
        return ResponseEntity.ok(ApiResponse.success(appointmentService.markNoShow(id, userId)));
    }

    // ── Queries ─────────────────────────────────────────────────────────

    @GetMapping("/member/{memberId}")
    @PreAuthorize("hasAnyRole('STUDIO_OWNER', 'MANAGER', 'TRAINER', 'MEMBER')")
    public ResponseEntity<ApiResponse<List<AppointmentDto>>> getMemberAppointments(
            @PathVariable UUID memberId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        Page<AppointmentDto> result = appointmentService.getMemberAppointments(memberId, PageRequest.of(page, size));
        return ResponseEntity.ok(ApiResponse.success(result.getContent(), PageMeta.from(result)));
    }

    @GetMapping("/staff/{staffId}")
    @PreAuthorize("hasAnyRole('STUDIO_OWNER', 'MANAGER', 'TRAINER')")
    public ResponseEntity<ApiResponse<List<AppointmentDto>>> getStaffAppointments(
            @PathVariable UUID staffId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        Page<AppointmentDto> result = appointmentService.getStaffAppointments(staffId, PageRequest.of(page, size));
        return ResponseEntity.ok(ApiResponse.success(result.getContent(), PageMeta.from(result)));
    }

    @GetMapping("/staff/{staffId}/agenda")
    @PreAuthorize("hasAnyRole('STUDIO_OWNER', 'MANAGER', 'TRAINER')")
    public ResponseEntity<ApiResponse<DayAgendaDto>> getStaffAgenda(
            @PathVariable UUID staffId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        return ResponseEntity.ok(ApiResponse.success(appointmentService.getStaffAgenda(staffId, date)));
    }
}

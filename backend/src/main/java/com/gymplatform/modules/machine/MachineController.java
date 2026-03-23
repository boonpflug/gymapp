package com.gymplatform.modules.machine;

import com.gymplatform.modules.machine.dto.*;
import com.gymplatform.shared.ApiResponse;
import com.gymplatform.shared.PageMeta;
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
@RequestMapping("/api/machines")
@RequiredArgsConstructor
public class MachineController {

    private final MachineService machineService;

    // ── Machines ──────────────────────────────────────────────────────────

    @GetMapping
    @PreAuthorize("hasAnyRole('STUDIO_OWNER', 'MANAGER', 'TRAINER')")
    public ResponseEntity<ApiResponse<List<MachineDto>>> listMachines(
            @RequestParam(required = false) UUID facilityId) {
        return ResponseEntity.ok(ApiResponse.success(machineService.listMachines(facilityId)));
    }

    @GetMapping("/computer-assisted")
    @PreAuthorize("hasAnyRole('STUDIO_OWNER', 'MANAGER', 'TRAINER')")
    public ResponseEntity<ApiResponse<List<MachineDto>>> getComputerAssistedMachines() {
        return ResponseEntity.ok(ApiResponse.success(machineService.getComputerAssistedMachines()));
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('STUDIO_OWNER', 'MANAGER')")
    public ResponseEntity<ApiResponse<MachineDto>> registerMachine(
            @Valid @RequestBody CreateMachineRequest request,
            @AuthenticationPrincipal UserDetails user) {
        UUID userId = UUID.fromString(user.getUsername());
        return ResponseEntity.ok(ApiResponse.success(machineService.registerMachine(request, userId)));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('STUDIO_OWNER', 'MANAGER', 'TRAINER')")
    public ResponseEntity<ApiResponse<MachineDto>> getMachine(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.success(machineService.getMachine(id)));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('STUDIO_OWNER', 'MANAGER')")
    public ResponseEntity<ApiResponse<MachineDto>> updateMachine(
            @PathVariable UUID id,
            @Valid @RequestBody CreateMachineRequest request,
            @AuthenticationPrincipal UserDetails user) {
        UUID userId = UUID.fromString(user.getUsername());
        return ResponseEntity.ok(ApiResponse.success(machineService.updateMachine(id, request, userId)));
    }

    @PutMapping("/{id}/status")
    @PreAuthorize("hasAnyRole('STUDIO_OWNER', 'MANAGER')")
    public ResponseEntity<ApiResponse<MachineDto>> updateMachineStatus(
            @PathVariable UUID id,
            @RequestParam MachineStatus status,
            @AuthenticationPrincipal UserDetails user) {
        UUID userId = UUID.fromString(user.getUsername());
        return ResponseEntity.ok(ApiResponse.success(machineService.updateMachineStatus(id, status, userId)));
    }

    // ── Maintenance ──────────────────────────────────────────────────────

    @PostMapping("/maintenance")
    @PreAuthorize("hasAnyRole('STUDIO_OWNER', 'MANAGER')")
    public ResponseEntity<ApiResponse<MachineMaintenanceLogDto>> logMaintenance(
            @Valid @RequestBody CreateMaintenanceLogRequest request,
            @AuthenticationPrincipal UserDetails user) {
        UUID userId = UUID.fromString(user.getUsername());
        return ResponseEntity.ok(ApiResponse.success(machineService.logMaintenance(request, userId)));
    }

    @GetMapping("/{machineId}/maintenance")
    @PreAuthorize("hasAnyRole('STUDIO_OWNER', 'MANAGER')")
    public ResponseEntity<ApiResponse<List<MachineMaintenanceLogDto>>> getMaintenanceLogs(
            @PathVariable UUID machineId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        Page<MachineMaintenanceLogDto> result = machineService.getMaintenanceLogs(machineId, PageRequest.of(page, size));
        return ResponseEntity.ok(ApiResponse.success(result.getContent(), PageMeta.from(result)));
    }

    // ── Sensor Sessions ──────────────────────────────────────────────────

    @PostMapping("/sensor-sessions")
    @PreAuthorize("hasAnyRole('STUDIO_OWNER', 'MANAGER', 'TRAINER')")
    public ResponseEntity<ApiResponse<MachineSensorSessionDto>> recordSensorSession(
            @Valid @RequestBody RecordSensorSessionRequest request,
            @AuthenticationPrincipal UserDetails user) {
        UUID userId = UUID.fromString(user.getUsername());
        return ResponseEntity.ok(ApiResponse.success(machineService.recordSensorSession(request, userId)));
    }

    @GetMapping("/sensor-sessions")
    @PreAuthorize("hasAnyRole('STUDIO_OWNER', 'MANAGER', 'TRAINER')")
    public ResponseEntity<ApiResponse<List<MachineSensorSessionDto>>> getSensorSessions(
            @RequestParam(required = false) UUID memberId,
            @RequestParam(required = false) UUID machineId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        Page<MachineSensorSessionDto> result = machineService.getSensorSessions(memberId, machineId, PageRequest.of(page, size));
        return ResponseEntity.ok(ApiResponse.success(result.getContent(), PageMeta.from(result)));
    }

    // ── Strength Measurements ────────────────────────────────────────────

    @PostMapping("/measurements")
    @PreAuthorize("hasAnyRole('STUDIO_OWNER', 'MANAGER', 'TRAINER')")
    public ResponseEntity<ApiResponse<StrengthMeasurementDto>> recordMeasurement(
            @Valid @RequestBody RecordMeasurementRequest request,
            @AuthenticationPrincipal UserDetails user) {
        UUID userId = UUID.fromString(user.getUsername());
        return ResponseEntity.ok(ApiResponse.success(machineService.recordMeasurement(request, userId)));
    }

    @GetMapping("/measurements/{memberId}")
    @PreAuthorize("hasAnyRole('STUDIO_OWNER', 'MANAGER', 'TRAINER', 'MEMBER')")
    public ResponseEntity<ApiResponse<List<StrengthMeasurementDto>>> getMemberMeasurements(
            @PathVariable UUID memberId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        Page<StrengthMeasurementDto> result = machineService.getMemberMeasurements(memberId, PageRequest.of(page, size));
        return ResponseEntity.ok(ApiResponse.success(result.getContent(), PageMeta.from(result)));
    }

    @GetMapping("/measurements/{memberId}/progress")
    @PreAuthorize("hasAnyRole('STUDIO_OWNER', 'MANAGER', 'TRAINER', 'MEMBER')")
    public ResponseEntity<ApiResponse<MemberProgressDto>> getMemberProgress(
            @PathVariable UUID memberId,
            @RequestParam UUID machineId,
            @RequestParam MeasurementType type) {
        return ResponseEntity.ok(ApiResponse.success(machineService.getMemberProgress(memberId, machineId, type)));
    }

    // ── Utilization ──────────────────────────────────────────────────────

    @GetMapping("/utilization")
    @PreAuthorize("hasAnyRole('STUDIO_OWNER', 'MANAGER')")
    public ResponseEntity<ApiResponse<List<MachineUtilizationDto>>> getMachineUtilization(
            @RequestParam(required = false) UUID facilityId) {
        return ResponseEntity.ok(ApiResponse.success(machineService.getMachineUtilization(facilityId)));
    }
}

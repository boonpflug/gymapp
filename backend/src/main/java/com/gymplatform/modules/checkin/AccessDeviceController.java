package com.gymplatform.modules.checkin;

import com.gymplatform.modules.checkin.dto.AccessDeviceDto;
import com.gymplatform.modules.checkin.dto.CreateAccessDeviceRequest;
import com.gymplatform.shared.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/devices")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('STUDIO_OWNER', 'MANAGER')")
public class AccessDeviceController {

    private final AccessDeviceService deviceService;

    @GetMapping
    public ResponseEntity<ApiResponse<List<AccessDeviceDto>>> getAllDevices() {
        return ResponseEntity.ok(ApiResponse.success(deviceService.getAllDevices()));
    }

    @GetMapping("/active")
    public ResponseEntity<ApiResponse<List<AccessDeviceDto>>> getActiveDevices() {
        return ResponseEntity.ok(ApiResponse.success(deviceService.getActiveDevices()));
    }

    @GetMapping("/{deviceId}")
    public ResponseEntity<ApiResponse<AccessDeviceDto>> getDevice(@PathVariable UUID deviceId) {
        return ResponseEntity.ok(ApiResponse.success(deviceService.getDevice(deviceId)));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<AccessDeviceDto>> createDevice(
            @Valid @RequestBody CreateAccessDeviceRequest request) {
        return ResponseEntity.ok(ApiResponse.success(deviceService.createDevice(request)));
    }

    @PutMapping("/{deviceId}")
    public ResponseEntity<ApiResponse<AccessDeviceDto>> updateDevice(
            @PathVariable UUID deviceId,
            @Valid @RequestBody CreateAccessDeviceRequest request) {
        return ResponseEntity.ok(ApiResponse.success(deviceService.updateDevice(deviceId, request)));
    }

    @PatchMapping("/{deviceId}/toggle")
    public ResponseEntity<ApiResponse<Void>> toggleDevice(
            @PathVariable UUID deviceId,
            @RequestParam boolean active) {
        deviceService.toggleDevice(deviceId, active);
        return ResponseEntity.ok(ApiResponse.success(null));
    }

    @GetMapping("/{deviceId}/status")
    public ResponseEntity<ApiResponse<DeviceStatus>> getDeviceStatus(@PathVariable UUID deviceId) {
        return ResponseEntity.ok(ApiResponse.success(deviceService.getDeviceStatus(deviceId)));
    }
}

package com.gymplatform.modules.checkin;

import com.gymplatform.modules.checkin.dto.*;
import com.gymplatform.shared.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/checkin")
@RequiredArgsConstructor
public class CheckInController {

    private final CheckInService checkInService;
    private final OccupancyService occupancyService;

    @PostMapping("/manual")
    @PreAuthorize("hasAnyRole('STUDIO_OWNER', 'MANAGER', 'RECEPTIONIST')")
    public ResponseEntity<ApiResponse<CheckInDto>> manualCheckIn(
            @Valid @RequestBody ManualCheckInRequest request,
            @AuthenticationPrincipal UserDetails user) {
        UUID staffId = UUID.fromString(user.getUsername());
        CheckInDto result = checkInService.manualCheckIn(request, staffId);
        return ResponseEntity.ok(ApiResponse.success(result));
    }

    @PostMapping("/device")
    @PreAuthorize("hasAnyRole('STUDIO_OWNER', 'MANAGER', 'RECEPTIONIST')")
    public ResponseEntity<ApiResponse<CheckInDto>> deviceCheckIn(
            @Valid @RequestBody DeviceCheckInRequest request) {
        CheckInDto result = checkInService.deviceCheckIn(request);
        return ResponseEntity.ok(ApiResponse.success(result));
    }

    @PostMapping("/checkout")
    @PreAuthorize("hasAnyRole('STUDIO_OWNER', 'MANAGER', 'RECEPTIONIST', 'MEMBER')")
    public ResponseEntity<ApiResponse<Void>> checkOut(
            @Valid @RequestBody CheckOutRequest request) {
        checkInService.checkOut(request);
        return ResponseEntity.ok(ApiResponse.success(null));
    }

    @GetMapping("/history/{memberId}")
    @PreAuthorize("hasAnyRole('STUDIO_OWNER', 'MANAGER', 'RECEPTIONIST', 'TRAINER')")
    public ResponseEntity<ApiResponse<Page<CheckInDto>>> getCheckInHistory(
            @PathVariable UUID memberId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        Pageable pageable = PageRequest.of(page, size);
        Page<CheckInDto> result = checkInService.getCheckInHistory(memberId, pageable);
        return ResponseEntity.ok(ApiResponse.success(result));
    }

    @GetMapping("/recent")
    @PreAuthorize("hasAnyRole('STUDIO_OWNER', 'MANAGER', 'RECEPTIONIST')")
    public ResponseEntity<ApiResponse<Page<CheckInDto>>> getRecentCheckIns(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        Pageable pageable = PageRequest.of(page, size);
        Page<CheckInDto> result = checkInService.getRecentCheckIns(pageable);
        return ResponseEntity.ok(ApiResponse.success(result));
    }

    @GetMapping("/current")
    @PreAuthorize("hasAnyRole('STUDIO_OWNER', 'MANAGER', 'RECEPTIONIST')")
    public ResponseEntity<ApiResponse<Page<CheckInDto>>> getCurrentlyCheckedIn(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        Pageable pageable = PageRequest.of(page, size);
        Page<CheckInDto> result = checkInService.getCurrentlyCheckedIn(pageable);
        return ResponseEntity.ok(ApiResponse.success(result));
    }

    @GetMapping("/occupancy")
    public ResponseEntity<ApiResponse<OccupancyDto>> getOccupancy() {
        OccupancyDto result = occupancyService.getCurrentOccupancy(null);
        return ResponseEntity.ok(ApiResponse.success(result));
    }
}

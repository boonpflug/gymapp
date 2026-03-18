package com.gymplatform.modules.checkin;

import com.gymplatform.modules.checkin.dto.AccessEventDto;
import com.gymplatform.shared.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/access-events")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('STUDIO_OWNER', 'MANAGER', 'RECEPTIONIST')")
public class AccessEventController {

    private final AccessEventService eventService;

    @GetMapping
    public ResponseEntity<ApiResponse<Page<AccessEventDto>>> getRecentEvents(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size) {
        Pageable pageable = PageRequest.of(page, size);
        return ResponseEntity.ok(ApiResponse.success(eventService.getRecentEvents(pageable)));
    }

    @GetMapping("/device/{deviceId}")
    public ResponseEntity<ApiResponse<Page<AccessEventDto>>> getEventsByDevice(
            @PathVariable UUID deviceId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size) {
        Pageable pageable = PageRequest.of(page, size);
        return ResponseEntity.ok(ApiResponse.success(eventService.getEventsByDevice(deviceId, pageable)));
    }

    @GetMapping("/member/{memberId}")
    public ResponseEntity<ApiResponse<Page<AccessEventDto>>> getEventsByMember(
            @PathVariable UUID memberId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size) {
        Pageable pageable = PageRequest.of(page, size);
        return ResponseEntity.ok(ApiResponse.success(eventService.getEventsByMember(memberId, pageable)));
    }
}

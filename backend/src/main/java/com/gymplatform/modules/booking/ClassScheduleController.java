package com.gymplatform.modules.booking;

import com.gymplatform.modules.booking.dto.ClassScheduleDto;
import com.gymplatform.modules.booking.dto.CreateScheduleRequest;
import com.gymplatform.shared.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/booking/schedules")
@RequiredArgsConstructor
public class ClassScheduleController {

    private final ClassScheduleService scheduleService;

    @GetMapping("/weekly")
    @PreAuthorize("hasAnyRole('STUDIO_OWNER', 'MANAGER', 'TRAINER', 'RECEPTIONIST', 'MEMBER')")
    public ResponseEntity<ApiResponse<List<ClassScheduleDto>>> getWeeklySchedule(
            @RequestParam Instant weekStart) {
        return ResponseEntity.ok(ApiResponse.success(scheduleService.getWeeklySchedule(weekStart)));
    }

    @GetMapping("/range")
    @PreAuthorize("hasAnyRole('STUDIO_OWNER', 'MANAGER', 'TRAINER', 'RECEPTIONIST', 'MEMBER')")
    public ResponseEntity<ApiResponse<List<ClassScheduleDto>>> getScheduleRange(
            @RequestParam Instant from,
            @RequestParam Instant to) {
        return ResponseEntity.ok(ApiResponse.success(scheduleService.getScheduleRange(from, to)));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('STUDIO_OWNER', 'MANAGER', 'TRAINER', 'RECEPTIONIST', 'MEMBER')")
    public ResponseEntity<ApiResponse<ClassScheduleDto>> getById(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.success(scheduleService.getById(id)));
    }

    @GetMapping("/trainer/{trainerId}")
    @PreAuthorize("hasAnyRole('STUDIO_OWNER', 'MANAGER', 'TRAINER')")
    public ResponseEntity<ApiResponse<List<ClassScheduleDto>>> getByTrainer(
            @PathVariable UUID trainerId,
            @RequestParam Instant from,
            @RequestParam Instant to) {
        return ResponseEntity.ok(ApiResponse.success(scheduleService.getByTrainer(trainerId, from, to)));
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('STUDIO_OWNER', 'MANAGER')")
    public ResponseEntity<ApiResponse<List<ClassScheduleDto>>> create(
            @Valid @RequestBody CreateScheduleRequest req) {
        return ResponseEntity.ok(ApiResponse.success(scheduleService.create(req)));
    }

    @PostMapping("/{id}/cancel")
    @PreAuthorize("hasAnyRole('STUDIO_OWNER', 'MANAGER')")
    public ResponseEntity<ApiResponse<ClassScheduleDto>> cancel(
            @PathVariable UUID id,
            @RequestBody Map<String, String> body) {
        String reason = body.getOrDefault("reason", "");
        return ResponseEntity.ok(ApiResponse.success(scheduleService.cancelSchedule(id, reason)));
    }
}

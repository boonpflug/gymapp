package com.gymplatform.modules.training;

import com.gymplatform.modules.training.dto.*;
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

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/training/sessions")
@RequiredArgsConstructor
public class TrainingSessionController {

    private final TrainingSessionService sessionService;

    @PostMapping
    @PreAuthorize("hasAnyRole('STUDIO_OWNER', 'MANAGER', 'TRAINER', 'MEMBER')")
    public ResponseEntity<ApiResponse<TrainingSessionDto>> startSession(
            @Valid @RequestBody StartSessionRequest req,
            @AuthenticationPrincipal UserDetails user) {
        UUID userId = UUID.fromString(user.getUsername());
        return ResponseEntity.ok(ApiResponse.success(sessionService.startSession(req, userId)));
    }

    @PostMapping("/{id}/finish")
    @PreAuthorize("hasAnyRole('STUDIO_OWNER', 'MANAGER', 'TRAINER', 'MEMBER')")
    public ResponseEntity<ApiResponse<TrainingSessionDto>> finishSession(
            @PathVariable UUID id,
            @Valid @RequestBody FinishSessionRequest req,
            @AuthenticationPrincipal UserDetails user) {
        UUID userId = UUID.fromString(user.getUsername());
        return ResponseEntity.ok(ApiResponse.success(sessionService.finishSession(id, req, userId)));
    }

    @PostMapping("/log")
    @PreAuthorize("hasAnyRole('STUDIO_OWNER', 'MANAGER', 'TRAINER', 'MEMBER')")
    public ResponseEntity<ApiResponse<TrainingLogDto>> logExercise(
            @Valid @RequestBody LogExerciseRequest req,
            @AuthenticationPrincipal UserDetails user) {
        UUID userId = UUID.fromString(user.getUsername());
        return ResponseEntity.ok(ApiResponse.success(sessionService.logExercise(req, userId)));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('STUDIO_OWNER', 'MANAGER', 'TRAINER', 'MEMBER')")
    public ResponseEntity<ApiResponse<TrainingSessionDto>> getById(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.success(sessionService.getById(id)));
    }

    @GetMapping("/member/{memberId}")
    @PreAuthorize("hasAnyRole('STUDIO_OWNER', 'MANAGER', 'TRAINER', 'MEMBER')")
    public ResponseEntity<ApiResponse<List<TrainingSessionDto>>> getMemberSessions(
            @PathVariable UUID memberId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        Page<TrainingSessionDto> result = sessionService.getMemberSessions(memberId, PageRequest.of(page, size));
        return ResponseEntity.ok(ApiResponse.success(result.getContent(), PageMeta.from(result)));
    }

    @GetMapping("/member/{memberId}/exercise/{exerciseId}/history")
    @PreAuthorize("hasAnyRole('STUDIO_OWNER', 'MANAGER', 'TRAINER', 'MEMBER')")
    public ResponseEntity<ApiResponse<List<TrainingLogDto>>> getExerciseHistory(
            @PathVariable UUID memberId,
            @PathVariable UUID exerciseId) {
        return ResponseEntity.ok(ApiResponse.success(
                sessionService.getExerciseHistory(memberId, exerciseId)));
    }

    @GetMapping("/member/{memberId}/count")
    @PreAuthorize("hasAnyRole('STUDIO_OWNER', 'MANAGER', 'TRAINER', 'MEMBER')")
    public ResponseEntity<ApiResponse<Long>> getSessionCount(
            @PathVariable UUID memberId,
            @RequestParam(defaultValue = "30") int days) {
        Instant since = Instant.now().minus(days, ChronoUnit.DAYS);
        return ResponseEntity.ok(ApiResponse.success(sessionService.getSessionCount(memberId, since)));
    }
}

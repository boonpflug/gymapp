package com.gymplatform.modules.training;

import com.gymplatform.modules.training.dto.CreateGoalRequest;
import com.gymplatform.modules.training.dto.TrainingGoalDto;
import com.gymplatform.modules.training.dto.UpdateGoalProgressRequest;
import com.gymplatform.shared.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/training/goals")
@RequiredArgsConstructor
public class TrainingGoalController {

    private final TrainingGoalService goalService;

    @PostMapping
    @PreAuthorize("hasAnyRole('STUDIO_OWNER', 'MANAGER', 'TRAINER', 'MEMBER')")
    public ResponseEntity<ApiResponse<TrainingGoalDto>> create(
            @Valid @RequestBody CreateGoalRequest req,
            @AuthenticationPrincipal UserDetails user) {
        UUID userId = UUID.fromString(user.getUsername());
        return ResponseEntity.ok(ApiResponse.success(goalService.create(req, userId)));
    }

    @PostMapping("/{id}/progress")
    @PreAuthorize("hasAnyRole('STUDIO_OWNER', 'MANAGER', 'TRAINER', 'MEMBER')")
    public ResponseEntity<ApiResponse<TrainingGoalDto>> updateProgress(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateGoalProgressRequest req,
            @AuthenticationPrincipal UserDetails user) {
        UUID userId = UUID.fromString(user.getUsername());
        return ResponseEntity.ok(ApiResponse.success(goalService.updateProgress(id, req, userId)));
    }

    @PostMapping("/{id}/abandon")
    @PreAuthorize("hasAnyRole('STUDIO_OWNER', 'MANAGER', 'TRAINER', 'MEMBER')")
    public ResponseEntity<ApiResponse<TrainingGoalDto>> abandon(
            @PathVariable UUID id,
            @AuthenticationPrincipal UserDetails user) {
        UUID userId = UUID.fromString(user.getUsername());
        return ResponseEntity.ok(ApiResponse.success(goalService.abandon(id, userId)));
    }

    @GetMapping("/member/{memberId}")
    @PreAuthorize("hasAnyRole('STUDIO_OWNER', 'MANAGER', 'TRAINER', 'MEMBER')")
    public ResponseEntity<ApiResponse<List<TrainingGoalDto>>> getMemberGoals(
            @PathVariable UUID memberId) {
        return ResponseEntity.ok(ApiResponse.success(goalService.getMemberGoals(memberId)));
    }

    @GetMapping("/member/{memberId}/active")
    @PreAuthorize("hasAnyRole('STUDIO_OWNER', 'MANAGER', 'TRAINER', 'MEMBER')")
    public ResponseEntity<ApiResponse<List<TrainingGoalDto>>> getMemberActiveGoals(
            @PathVariable UUID memberId) {
        return ResponseEntity.ok(ApiResponse.success(goalService.getMemberActiveGoals(memberId)));
    }
}

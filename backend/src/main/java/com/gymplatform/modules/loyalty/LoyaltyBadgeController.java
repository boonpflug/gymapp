package com.gymplatform.modules.loyalty;

import com.gymplatform.modules.loyalty.dto.*;
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
@RequestMapping("/api/loyalty/badges")
@RequiredArgsConstructor
public class LoyaltyBadgeController {

    private final LoyaltyBadgeService loyaltyBadgeService;

    @GetMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<List<LoyaltyBadgeDto>>> listBadges() {
        return ResponseEntity.ok(ApiResponse.success(loyaltyBadgeService.listBadges()));
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('STUDIO_OWNER', 'MANAGER')")
    public ResponseEntity<ApiResponse<LoyaltyBadgeDto>> createBadge(
            @Valid @RequestBody CreateBadgeRequest req,
            @AuthenticationPrincipal UserDetails user) {
        UUID userId = UUID.fromString(user.getUsername());
        return ResponseEntity.ok(ApiResponse.success(loyaltyBadgeService.createBadge(req, userId)));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('STUDIO_OWNER', 'MANAGER')")
    public ResponseEntity<ApiResponse<LoyaltyBadgeDto>> updateBadge(
            @PathVariable UUID id,
            @Valid @RequestBody CreateBadgeRequest req,
            @AuthenticationPrincipal UserDetails user) {
        UUID userId = UUID.fromString(user.getUsername());
        return ResponseEntity.ok(ApiResponse.success(loyaltyBadgeService.updateBadge(id, req, userId)));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('STUDIO_OWNER', 'MANAGER')")
    public ResponseEntity<ApiResponse<Void>> deactivateBadge(
            @PathVariable UUID id,
            @AuthenticationPrincipal UserDetails user) {
        UUID userId = UUID.fromString(user.getUsername());
        loyaltyBadgeService.deactivateBadge(id, userId);
        return ResponseEntity.ok(ApiResponse.success(null));
    }

    @GetMapping("/member/{memberId}")
    @PreAuthorize("hasAnyRole('STUDIO_OWNER', 'MANAGER', 'TRAINER', 'MEMBER')")
    public ResponseEntity<ApiResponse<List<MemberBadgeDto>>> getMemberBadges(@PathVariable UUID memberId) {
        return ResponseEntity.ok(ApiResponse.success(loyaltyBadgeService.getMemberBadges(memberId)));
    }

    @GetMapping("/streaks/{memberId}")
    @PreAuthorize("hasAnyRole('STUDIO_OWNER', 'MANAGER', 'TRAINER', 'MEMBER')")
    public ResponseEntity<ApiResponse<List<MemberStreakDto>>> getMemberStreaks(@PathVariable UUID memberId) {
        return ResponseEntity.ok(ApiResponse.success(loyaltyBadgeService.getMemberStreaks(memberId)));
    }

    @GetMapping("/streaks/leaderboard")
    @PreAuthorize("hasAnyRole('STUDIO_OWNER', 'MANAGER')")
    public ResponseEntity<ApiResponse<List<MemberStreakDto>>> getStreakLeaderboard(
            @RequestParam StreakType streakType) {
        return ResponseEntity.ok(ApiResponse.success(loyaltyBadgeService.getStreakLeaderboard(streakType)));
    }
}

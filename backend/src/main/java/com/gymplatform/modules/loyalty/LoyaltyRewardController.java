package com.gymplatform.modules.loyalty;

import com.gymplatform.modules.loyalty.dto.*;
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
@RequestMapping("/api/loyalty/rewards")
@RequiredArgsConstructor
public class LoyaltyRewardController {

    private final LoyaltyRewardService loyaltyRewardService;

    @GetMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<List<LoyaltyRewardDto>>> listRewards(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        Page<LoyaltyRewardDto> result = loyaltyRewardService.listRewards(PageRequest.of(page, size));
        return ResponseEntity.ok(ApiResponse.success(result.getContent(), PageMeta.from(result)));
    }

    @GetMapping("/active")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<List<LoyaltyRewardDto>>> listActiveRewards() {
        return ResponseEntity.ok(ApiResponse.success(loyaltyRewardService.listActiveRewards()));
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('STUDIO_OWNER', 'MANAGER')")
    public ResponseEntity<ApiResponse<LoyaltyRewardDto>> createReward(
            @Valid @RequestBody CreateRewardRequest req,
            @AuthenticationPrincipal UserDetails user) {
        UUID userId = UUID.fromString(user.getUsername());
        return ResponseEntity.ok(ApiResponse.success(loyaltyRewardService.createReward(req, userId)));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('STUDIO_OWNER', 'MANAGER')")
    public ResponseEntity<ApiResponse<LoyaltyRewardDto>> updateReward(
            @PathVariable UUID id,
            @Valid @RequestBody CreateRewardRequest req,
            @AuthenticationPrincipal UserDetails user) {
        UUID userId = UUID.fromString(user.getUsername());
        return ResponseEntity.ok(ApiResponse.success(loyaltyRewardService.updateReward(id, req, userId)));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('STUDIO_OWNER', 'MANAGER')")
    public ResponseEntity<ApiResponse<Void>> deactivateReward(
            @PathVariable UUID id,
            @AuthenticationPrincipal UserDetails user) {
        UUID userId = UUID.fromString(user.getUsername());
        loyaltyRewardService.deactivateReward(id, userId);
        return ResponseEntity.ok(ApiResponse.success(null));
    }

    @PostMapping("/redeem")
    @PreAuthorize("hasAnyRole('STUDIO_OWNER', 'MANAGER', 'MEMBER')")
    public ResponseEntity<ApiResponse<LoyaltyRedemptionDto>> redeemReward(
            @Valid @RequestBody RedeemRewardRequest req,
            @AuthenticationPrincipal UserDetails user) {
        UUID userId = UUID.fromString(user.getUsername());
        return ResponseEntity.ok(ApiResponse.success(loyaltyRewardService.redeemReward(req.getMemberId(), req.getRewardId(), userId)));
    }

    @GetMapping("/redemptions/{memberId}")
    @PreAuthorize("hasAnyRole('STUDIO_OWNER', 'MANAGER', 'TRAINER', 'MEMBER')")
    public ResponseEntity<ApiResponse<List<LoyaltyRedemptionDto>>> getMemberRedemptions(
            @PathVariable UUID memberId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        Page<LoyaltyRedemptionDto> result = loyaltyRewardService.getMemberRedemptions(memberId, PageRequest.of(page, size));
        return ResponseEntity.ok(ApiResponse.success(result.getContent(), PageMeta.from(result)));
    }

    @PostMapping("/redemptions/{id}/fulfill")
    @PreAuthorize("hasAnyRole('STUDIO_OWNER', 'MANAGER')")
    public ResponseEntity<ApiResponse<LoyaltyRedemptionDto>> fulfillRedemption(
            @PathVariable UUID id,
            @AuthenticationPrincipal UserDetails user) {
        UUID userId = UUID.fromString(user.getUsername());
        return ResponseEntity.ok(ApiResponse.success(loyaltyRewardService.fulfillRedemption(id, userId)));
    }

    @PostMapping("/redemptions/{id}/cancel")
    @PreAuthorize("hasAnyRole('STUDIO_OWNER', 'MANAGER')")
    public ResponseEntity<ApiResponse<LoyaltyRedemptionDto>> cancelRedemption(
            @PathVariable UUID id,
            @AuthenticationPrincipal UserDetails user) {
        UUID userId = UUID.fromString(user.getUsername());
        return ResponseEntity.ok(ApiResponse.success(loyaltyRewardService.cancelRedemption(id, userId)));
    }
}

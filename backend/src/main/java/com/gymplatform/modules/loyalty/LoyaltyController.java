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
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/loyalty")
@RequiredArgsConstructor
public class LoyaltyController {

    private final LoyaltyPointsService loyaltyPointsService;

    @GetMapping("/points/{memberId}")
    @PreAuthorize("hasAnyRole('STUDIO_OWNER', 'MANAGER', 'TRAINER')")
    public ResponseEntity<ApiResponse<Integer>> getBalance(@PathVariable UUID memberId) {
        return ResponseEntity.ok(ApiResponse.success(loyaltyPointsService.getBalance(memberId)));
    }

    @GetMapping("/points/{memberId}/history")
    @PreAuthorize("hasAnyRole('STUDIO_OWNER', 'MANAGER', 'TRAINER')")
    public ResponseEntity<ApiResponse<List<LoyaltyTransactionDto>>> getTransactionHistory(
            @PathVariable UUID memberId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        Page<LoyaltyTransactionDto> result = loyaltyPointsService.getTransactionHistory(memberId, PageRequest.of(page, size));
        return ResponseEntity.ok(ApiResponse.success(result.getContent(), PageMeta.from(result)));
    }

    @PostMapping("/points/award")
    @PreAuthorize("hasAnyRole('STUDIO_OWNER', 'MANAGER')")
    public ResponseEntity<ApiResponse<LoyaltyTransactionDto>> awardPoints(
            @Valid @RequestBody AwardPointsRequest req,
            @AuthenticationPrincipal UserDetails user) {
        UUID userId = UUID.fromString(user.getUsername());
        return ResponseEntity.ok(ApiResponse.success(loyaltyPointsService.awardPoints(
                req.getMemberId(), req.getPoints(), req.getAction(), req.getReferenceId(), req.getDescription())));
    }

    @GetMapping("/members/{memberId}/summary")
    @PreAuthorize("hasAnyRole('STUDIO_OWNER', 'MANAGER', 'TRAINER', 'MEMBER')")
    public ResponseEntity<ApiResponse<MemberLoyaltySummaryDto>> getMemberSummary(@PathVariable UUID memberId) {
        return ResponseEntity.ok(ApiResponse.success(loyaltyPointsService.getMemberSummary(memberId)));
    }

    @GetMapping("/tiers")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<List<LoyaltyTierDto>>> listTiers() {
        return ResponseEntity.ok(ApiResponse.success(loyaltyPointsService.listActiveTiers()));
    }

    @PostMapping("/tiers")
    @PreAuthorize("hasRole('STUDIO_OWNER')")
    public ResponseEntity<ApiResponse<LoyaltyTierDto>> createTier(
            @Valid @RequestBody CreateLoyaltyTierRequest req,
            @AuthenticationPrincipal UserDetails user) {
        UUID userId = UUID.fromString(user.getUsername());
        return ResponseEntity.ok(ApiResponse.success(loyaltyPointsService.createTier(req, userId)));
    }

    @PutMapping("/tiers/{id}")
    @PreAuthorize("hasRole('STUDIO_OWNER')")
    public ResponseEntity<ApiResponse<LoyaltyTierDto>> updateTier(
            @PathVariable UUID id,
            @Valid @RequestBody CreateLoyaltyTierRequest req,
            @AuthenticationPrincipal UserDetails user) {
        UUID userId = UUID.fromString(user.getUsername());
        return ResponseEntity.ok(ApiResponse.success(loyaltyPointsService.updateTier(id, req, userId)));
    }

    @GetMapping("/config")
    @PreAuthorize("hasAnyRole('STUDIO_OWNER', 'MANAGER')")
    public ResponseEntity<ApiResponse<Map<LoyaltyAction, Integer>>> getPointsConfig() {
        return ResponseEntity.ok(ApiResponse.success(loyaltyPointsService.getPointsConfig()));
    }

    @PutMapping("/config/{action}")
    @PreAuthorize("hasRole('STUDIO_OWNER')")
    public ResponseEntity<ApiResponse<Void>> setPointsConfig(
            @PathVariable LoyaltyAction action,
            @RequestParam int points) {
        loyaltyPointsService.setPointsConfig(action, points);
        return ResponseEntity.ok(ApiResponse.success(null));
    }

    @GetMapping("/dashboard")
    @PreAuthorize("hasAnyRole('STUDIO_OWNER', 'MANAGER')")
    public ResponseEntity<ApiResponse<LoyaltyDashboardDto>> getDashboard() {
        return ResponseEntity.ok(ApiResponse.success(loyaltyPointsService.getDashboard()));
    }
}

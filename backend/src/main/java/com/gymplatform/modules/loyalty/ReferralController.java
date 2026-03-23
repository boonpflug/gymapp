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
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/loyalty/referrals")
@RequiredArgsConstructor
public class ReferralController {

    private final ReferralService referralService;

    @PostMapping
    @PreAuthorize("hasAnyRole('STUDIO_OWNER', 'MANAGER', 'MEMBER')")
    public ResponseEntity<ApiResponse<ReferralDto>> createReferral(
            @Valid @RequestBody CreateReferralRequest req,
            @AuthenticationPrincipal UserDetails user) {
        UUID userId = UUID.fromString(user.getUsername());
        return ResponseEntity.ok(ApiResponse.success(referralService.createReferral(req.getReferrerMemberId(), req.getReferredEmail())));
    }

    @GetMapping("/code/{code}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<ReferralDto>> getReferralByCode(@PathVariable String code) {
        return ResponseEntity.ok(ApiResponse.success(referralService.getReferralByCode(code)));
    }

    @GetMapping("/member/{memberId}")
    @PreAuthorize("hasAnyRole('STUDIO_OWNER', 'MANAGER', 'MEMBER')")
    public ResponseEntity<ApiResponse<List<ReferralDto>>> getMemberReferrals(@PathVariable UUID memberId) {
        return ResponseEntity.ok(ApiResponse.success(referralService.getMemberReferrals(memberId)));
    }

    @PostMapping("/{code}/convert")
    @PreAuthorize("hasAnyRole('STUDIO_OWNER', 'MANAGER')")
    public ResponseEntity<ApiResponse<ReferralDto>> convertReferral(
            @PathVariable String code,
            @RequestParam UUID newMemberId,
            @AuthenticationPrincipal UserDetails user) {
        UUID userId = UUID.fromString(user.getUsername());
        return ResponseEntity.ok(ApiResponse.success(referralService.convertReferral(code, newMemberId)));
    }

    @GetMapping("/stats/{memberId}")
    @PreAuthorize("hasAnyRole('STUDIO_OWNER', 'MANAGER', 'MEMBER')")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getReferralStats(@PathVariable UUID memberId) {
        return ResponseEntity.ok(ApiResponse.success(referralService.getReferralStats(memberId)));
    }
}

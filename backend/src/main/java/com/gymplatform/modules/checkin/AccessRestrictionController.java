package com.gymplatform.modules.checkin;

import com.gymplatform.modules.checkin.dto.AccessRestrictionDto;
import com.gymplatform.modules.checkin.dto.CreateRestrictionRequest;
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
@RequestMapping("/api/restrictions")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('STUDIO_OWNER', 'MANAGER')")
public class AccessRestrictionController {

    private final AccessRestrictionService restrictionService;

    @GetMapping("/member/{memberId}")
    public ResponseEntity<ApiResponse<List<AccessRestrictionDto>>> getMemberRestrictions(
            @PathVariable UUID memberId,
            @RequestParam(defaultValue = "true") boolean activeOnly) {
        List<AccessRestrictionDto> restrictions = activeOnly
                ? restrictionService.getActiveRestrictions(memberId)
                : restrictionService.getAllRestrictions(memberId);
        return ResponseEntity.ok(ApiResponse.success(restrictions));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<AccessRestrictionDto>> createRestriction(
            @Valid @RequestBody CreateRestrictionRequest request,
            @AuthenticationPrincipal UserDetails user) {
        UUID staffId = UUID.fromString(user.getUsername());
        return ResponseEntity.ok(ApiResponse.success(
                restrictionService.createRestriction(request, staffId)));
    }

    @PostMapping("/{restrictionId}/release")
    public ResponseEntity<ApiResponse<AccessRestrictionDto>> releaseRestriction(
            @PathVariable UUID restrictionId,
            @AuthenticationPrincipal UserDetails user) {
        UUID staffId = UUID.fromString(user.getUsername());
        return ResponseEntity.ok(ApiResponse.success(
                restrictionService.releaseRestriction(restrictionId, staffId)));
    }
}

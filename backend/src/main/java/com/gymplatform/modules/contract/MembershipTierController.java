package com.gymplatform.modules.contract;

import com.gymplatform.modules.contract.dto.CreateMembershipTierRequest;
import com.gymplatform.modules.contract.dto.MembershipTierDto;
import com.gymplatform.shared.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/membership-tiers")
@PreAuthorize("hasAnyRole('STUDIO_OWNER', 'MANAGER')")
@RequiredArgsConstructor
public class MembershipTierController {

    private final MembershipTierService membershipTierService;

    @PostMapping
    public ResponseEntity<ApiResponse<MembershipTierDto>> createTier(
            @Valid @RequestBody CreateMembershipTierRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(membershipTierService.createTier(request)));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<MembershipTierDto>>> getActiveTiers() {
        return ResponseEntity.ok(ApiResponse.success(membershipTierService.getActiveTiers()));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<MembershipTierDto>> getTier(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.success(membershipTierService.getTier(id)));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<MembershipTierDto>> updateTier(
            @PathVariable UUID id,
            @Valid @RequestBody CreateMembershipTierRequest request) {
        return ResponseEntity.ok(ApiResponse.success(membershipTierService.updateTier(id, request)));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteTier(@PathVariable UUID id) {
        membershipTierService.deleteTier(id);
        return ResponseEntity.ok(ApiResponse.success(null));
    }
}

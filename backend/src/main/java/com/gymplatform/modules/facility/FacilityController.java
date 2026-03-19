package com.gymplatform.modules.facility;

import com.gymplatform.modules.facility.dto.*;
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
@RequestMapping("/api/facilities")
@RequiredArgsConstructor
public class FacilityController {

    private final FacilityService facilityService;
    private final ConsolidatedDashboardService dashboardService;

    @GetMapping
    @PreAuthorize("hasAnyRole('STUDIO_OWNER', 'MANAGER')")
    public ResponseEntity<ApiResponse<List<FacilityDto>>> getAll(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        Page<FacilityDto> result = facilityService.getAll(PageRequest.of(page, size));
        return ResponseEntity.ok(ApiResponse.success(result.getContent(), PageMeta.from(result)));
    }

    @GetMapping("/list")
    @PreAuthorize("hasAnyRole('STUDIO_OWNER', 'MANAGER', 'TRAINER', 'RECEPTIONIST')")
    public ResponseEntity<ApiResponse<List<FacilityDto>>> getAllList() {
        return ResponseEntity.ok(ApiResponse.success(facilityService.getAllList()));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('STUDIO_OWNER', 'MANAGER')")
    public ResponseEntity<ApiResponse<FacilityDto>> getById(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.success(facilityService.getById(id)));
    }

    @PostMapping
    @PreAuthorize("hasRole('STUDIO_OWNER')")
    public ResponseEntity<ApiResponse<FacilityDto>> create(
            @Valid @RequestBody CreateFacilityRequest req,
            @AuthenticationPrincipal UserDetails user) {
        UUID userId = UUID.fromString(user.getUsername());
        return ResponseEntity.ok(ApiResponse.success(facilityService.create(req, userId)));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('STUDIO_OWNER')")
    public ResponseEntity<ApiResponse<FacilityDto>> update(
            @PathVariable UUID id,
            @Valid @RequestBody CreateFacilityRequest req,
            @AuthenticationPrincipal UserDetails user) {
        UUID userId = UUID.fromString(user.getUsername());
        return ResponseEntity.ok(ApiResponse.success(facilityService.update(id, req, userId)));
    }

    @PostMapping("/{id}/deactivate")
    @PreAuthorize("hasRole('STUDIO_OWNER')")
    public ResponseEntity<ApiResponse<Void>> deactivate(
            @PathVariable UUID id,
            @AuthenticationPrincipal UserDetails user) {
        UUID userId = UUID.fromString(user.getUsername());
        facilityService.deactivate(id, userId);
        return ResponseEntity.ok(ApiResponse.success(null));
    }

    // --- Configuration endpoints ---

    @GetMapping("/{id}/config")
    @PreAuthorize("hasAnyRole('STUDIO_OWNER', 'MANAGER')")
    public ResponseEntity<ApiResponse<List<FacilityConfigDto>>> getConfigurations(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.success(facilityService.getConfigurations(id)));
    }

    @PostMapping("/{id}/config")
    @PreAuthorize("hasRole('STUDIO_OWNER')")
    public ResponseEntity<ApiResponse<FacilityConfigDto>> setConfiguration(
            @PathVariable UUID id,
            @Valid @RequestBody SetFacilityConfigRequest req,
            @AuthenticationPrincipal UserDetails user) {
        UUID userId = UUID.fromString(user.getUsername());
        return ResponseEntity.ok(ApiResponse.success(facilityService.setConfiguration(id, req, userId)));
    }

    @DeleteMapping("/{facilityId}/config/{configKey}")
    @PreAuthorize("hasRole('STUDIO_OWNER')")
    public ResponseEntity<ApiResponse<Void>> deleteConfiguration(
            @PathVariable UUID facilityId,
            @PathVariable String configKey,
            @AuthenticationPrincipal UserDetails user) {
        UUID userId = UUID.fromString(user.getUsername());
        facilityService.deleteConfiguration(facilityId, configKey, userId);
        return ResponseEntity.ok(ApiResponse.success(null));
    }

    // --- Member facility access endpoints ---

    @GetMapping("/members/{memberId}/access")
    @PreAuthorize("hasAnyRole('STUDIO_OWNER', 'MANAGER', 'RECEPTIONIST')")
    public ResponseEntity<ApiResponse<List<MemberFacilityAccessDto>>> getMemberAccess(
            @PathVariable UUID memberId) {
        return ResponseEntity.ok(ApiResponse.success(facilityService.getMemberAccess(memberId)));
    }

    @GetMapping("/{facilityId}/members")
    @PreAuthorize("hasAnyRole('STUDIO_OWNER', 'MANAGER')")
    public ResponseEntity<ApiResponse<List<MemberFacilityAccessDto>>> getFacilityMembers(
            @PathVariable UUID facilityId) {
        return ResponseEntity.ok(ApiResponse.success(facilityService.getFacilityMembers(facilityId)));
    }

    @PostMapping("/members/access")
    @PreAuthorize("hasAnyRole('STUDIO_OWNER', 'MANAGER')")
    public ResponseEntity<ApiResponse<MemberFacilityAccessDto>> assignMember(
            @Valid @RequestBody AssignMemberFacilityRequest req,
            @AuthenticationPrincipal UserDetails user) {
        UUID userId = UUID.fromString(user.getUsername());
        return ResponseEntity.ok(ApiResponse.success(facilityService.assignMemberToFacility(req, userId)));
    }

    @DeleteMapping("/members/{memberId}/access/{facilityId}")
    @PreAuthorize("hasAnyRole('STUDIO_OWNER', 'MANAGER')")
    public ResponseEntity<ApiResponse<Void>> removeMemberAccess(
            @PathVariable UUID memberId,
            @PathVariable UUID facilityId,
            @AuthenticationPrincipal UserDetails user) {
        UUID userId = UUID.fromString(user.getUsername());
        facilityService.removeMemberFromFacility(memberId, facilityId, userId);
        return ResponseEntity.ok(ApiResponse.success(null));
    }

    @GetMapping("/members/{memberId}/can-access/{facilityId}")
    @PreAuthorize("hasAnyRole('STUDIO_OWNER', 'MANAGER', 'RECEPTIONIST', 'TRAINER')")
    public ResponseEntity<ApiResponse<Boolean>> canMemberAccess(
            @PathVariable UUID memberId,
            @PathVariable UUID facilityId) {
        return ResponseEntity.ok(ApiResponse.success(facilityService.canMemberAccessFacility(memberId, facilityId)));
    }

    // --- Consolidated dashboard ---

    @GetMapping("/dashboard/consolidated")
    @PreAuthorize("hasRole('STUDIO_OWNER')")
    public ResponseEntity<ApiResponse<ConsolidatedDashboardDto>> getConsolidatedDashboard() {
        return ResponseEntity.ok(ApiResponse.success(dashboardService.getDashboard()));
    }
}

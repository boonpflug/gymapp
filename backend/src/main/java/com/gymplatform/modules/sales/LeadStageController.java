package com.gymplatform.modules.sales;

import com.gymplatform.modules.sales.dto.CreateLeadStageRequest;
import com.gymplatform.modules.sales.dto.LeadStageDto;
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
@RequestMapping("/api/sales/stages")
@RequiredArgsConstructor
public class LeadStageController {

    private final LeadStageService stageService;

    @GetMapping
    @PreAuthorize("hasAnyRole('STUDIO_OWNER', 'MANAGER', 'RECEPTIONIST')")
    public ResponseEntity<ApiResponse<List<LeadStageDto>>> getAll() {
        return ResponseEntity.ok(ApiResponse.success(stageService.getAll()));
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('STUDIO_OWNER', 'MANAGER')")
    public ResponseEntity<ApiResponse<LeadStageDto>> create(
            @Valid @RequestBody CreateLeadStageRequest req,
            @AuthenticationPrincipal UserDetails user) {
        UUID userId = UUID.fromString(user.getUsername());
        return ResponseEntity.ok(ApiResponse.success(stageService.create(req, userId)));
    }

    @PostMapping("/{id}")
    @PreAuthorize("hasAnyRole('STUDIO_OWNER', 'MANAGER')")
    public ResponseEntity<ApiResponse<LeadStageDto>> update(
            @PathVariable UUID id,
            @Valid @RequestBody CreateLeadStageRequest req,
            @AuthenticationPrincipal UserDetails user) {
        UUID userId = UUID.fromString(user.getUsername());
        return ResponseEntity.ok(ApiResponse.success(stageService.update(id, req, userId)));
    }

    @PostMapping("/{id}/delete")
    @PreAuthorize("hasAnyRole('STUDIO_OWNER', 'MANAGER')")
    public ResponseEntity<ApiResponse<Void>> delete(
            @PathVariable UUID id,
            @AuthenticationPrincipal UserDetails user) {
        UUID userId = UUID.fromString(user.getUsername());
        stageService.delete(id, userId);
        return ResponseEntity.ok(ApiResponse.success(null));
    }

    @PostMapping("/init-defaults")
    @PreAuthorize("hasAnyRole('STUDIO_OWNER', 'MANAGER')")
    public ResponseEntity<ApiResponse<List<LeadStageDto>>> initDefaults(
            @AuthenticationPrincipal UserDetails user) {
        UUID userId = UUID.fromString(user.getUsername());
        stageService.initDefaultStages(userId);
        return ResponseEntity.ok(ApiResponse.success(stageService.getAll()));
    }
}

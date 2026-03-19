package com.gymplatform.modules.sales;

import com.gymplatform.modules.sales.dto.*;
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
@RequestMapping("/api/sales/leads")
@RequiredArgsConstructor
public class LeadController {

    private final LeadService leadService;

    @GetMapping
    @PreAuthorize("hasAnyRole('STUDIO_OWNER', 'MANAGER', 'RECEPTIONIST')")
    public ResponseEntity<ApiResponse<List<LeadDto>>> search(
            @RequestParam(required = false) String name,
            @RequestParam(required = false) LeadSource source,
            @RequestParam(required = false) UUID stageId,
            @RequestParam(required = false) UUID assignedStaffId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        Page<LeadDto> result = leadService.search(name, source, stageId, assignedStaffId,
                PageRequest.of(page, size));
        return ResponseEntity.ok(ApiResponse.success(result.getContent(), PageMeta.from(result)));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('STUDIO_OWNER', 'MANAGER', 'RECEPTIONIST')")
    public ResponseEntity<ApiResponse<LeadDto>> getById(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.success(leadService.getById(id)));
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('STUDIO_OWNER', 'MANAGER', 'RECEPTIONIST')")
    public ResponseEntity<ApiResponse<LeadDto>> create(
            @Valid @RequestBody CreateLeadRequest req,
            @AuthenticationPrincipal UserDetails user) {
        UUID userId = UUID.fromString(user.getUsername());
        return ResponseEntity.ok(ApiResponse.success(leadService.create(req, userId)));
    }

    @PostMapping("/{id}")
    @PreAuthorize("hasAnyRole('STUDIO_OWNER', 'MANAGER', 'RECEPTIONIST')")
    public ResponseEntity<ApiResponse<LeadDto>> update(
            @PathVariable UUID id,
            @Valid @RequestBody CreateLeadRequest req,
            @AuthenticationPrincipal UserDetails user) {
        UUID userId = UUID.fromString(user.getUsername());
        return ResponseEntity.ok(ApiResponse.success(leadService.update(id, req, userId)));
    }

    @PostMapping("/{id}/move")
    @PreAuthorize("hasAnyRole('STUDIO_OWNER', 'MANAGER', 'RECEPTIONIST')")
    public ResponseEntity<ApiResponse<LeadDto>> moveToStage(
            @PathVariable UUID id,
            @Valid @RequestBody MoveLeadRequest req,
            @AuthenticationPrincipal UserDetails user) {
        UUID userId = UUID.fromString(user.getUsername());
        return ResponseEntity.ok(ApiResponse.success(leadService.moveToStage(id, req.getStageId(), userId)));
    }

    @PostMapping("/{id}/assign")
    @PreAuthorize("hasAnyRole('STUDIO_OWNER', 'MANAGER')")
    public ResponseEntity<ApiResponse<LeadDto>> assignStaff(
            @PathVariable UUID id,
            @RequestParam UUID staffId,
            @AuthenticationPrincipal UserDetails user) {
        UUID userId = UUID.fromString(user.getUsername());
        return ResponseEntity.ok(ApiResponse.success(leadService.assignStaff(id, staffId, userId)));
    }

    @PostMapping("/{id}/convert")
    @PreAuthorize("hasAnyRole('STUDIO_OWNER', 'MANAGER')")
    public ResponseEntity<ApiResponse<LeadDto>> convert(
            @PathVariable UUID id,
            @RequestBody ConvertLeadRequest req,
            @AuthenticationPrincipal UserDetails user) {
        UUID userId = UUID.fromString(user.getUsername());
        return ResponseEntity.ok(ApiResponse.success(leadService.convertToMember(id, req, userId)));
    }

    @GetMapping("/pipeline")
    @PreAuthorize("hasAnyRole('STUDIO_OWNER', 'MANAGER')")
    public ResponseEntity<ApiResponse<SalesPipelineDto>> getPipeline() {
        return ResponseEntity.ok(ApiResponse.success(leadService.getPipeline()));
    }
}

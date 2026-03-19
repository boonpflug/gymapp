package com.gymplatform.modules.marketing;

import com.gymplatform.modules.marketing.dto.*;
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
@RequestMapping("/api/marketing/campaigns")
@RequiredArgsConstructor
public class CampaignController {

    private final CampaignService campaignService;
    private final AudienceBuilderService audienceBuilderService;

    @GetMapping
    @PreAuthorize("hasAnyRole('STUDIO_OWNER', 'MANAGER')")
    public ResponseEntity<ApiResponse<List<CampaignDto>>> list(
            @RequestParam(required = false) CampaignStatus status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        Page<CampaignDto> result = campaignService.list(status, PageRequest.of(page, size));
        return ResponseEntity.ok(ApiResponse.success(result.getContent(), PageMeta.from(result)));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('STUDIO_OWNER', 'MANAGER')")
    public ResponseEntity<ApiResponse<CampaignDto>> getById(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.success(campaignService.getById(id)));
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('STUDIO_OWNER', 'MANAGER')")
    public ResponseEntity<ApiResponse<CampaignDto>> create(
            @Valid @RequestBody CreateCampaignRequest req,
            @AuthenticationPrincipal UserDetails user) {
        UUID userId = UUID.fromString(user.getUsername());
        return ResponseEntity.ok(ApiResponse.success(campaignService.create(req, userId)));
    }

    @PostMapping("/{id}")
    @PreAuthorize("hasAnyRole('STUDIO_OWNER', 'MANAGER')")
    public ResponseEntity<ApiResponse<CampaignDto>> update(
            @PathVariable UUID id,
            @Valid @RequestBody CreateCampaignRequest req,
            @AuthenticationPrincipal UserDetails user) {
        UUID userId = UUID.fromString(user.getUsername());
        return ResponseEntity.ok(ApiResponse.success(campaignService.update(id, req, userId)));
    }

    @PostMapping("/{id}/schedule")
    @PreAuthorize("hasAnyRole('STUDIO_OWNER', 'MANAGER')")
    public ResponseEntity<ApiResponse<CampaignDto>> schedule(
            @PathVariable UUID id,
            @AuthenticationPrincipal UserDetails user) {
        UUID userId = UUID.fromString(user.getUsername());
        return ResponseEntity.ok(ApiResponse.success(campaignService.schedule(id, userId)));
    }

    @PostMapping("/{id}/send")
    @PreAuthorize("hasAnyRole('STUDIO_OWNER', 'MANAGER')")
    public ResponseEntity<ApiResponse<CampaignDto>> sendNow(
            @PathVariable UUID id,
            @AuthenticationPrincipal UserDetails user) {
        UUID userId = UUID.fromString(user.getUsername());
        return ResponseEntity.ok(ApiResponse.success(campaignService.sendNow(id, userId)));
    }

    @PostMapping("/{id}/cancel")
    @PreAuthorize("hasAnyRole('STUDIO_OWNER', 'MANAGER')")
    public ResponseEntity<ApiResponse<CampaignDto>> cancel(
            @PathVariable UUID id,
            @AuthenticationPrincipal UserDetails user) {
        UUID userId = UUID.fromString(user.getUsername());
        return ResponseEntity.ok(ApiResponse.success(campaignService.cancel(id, userId)));
    }

    @GetMapping("/{id}/recipients")
    @PreAuthorize("hasAnyRole('STUDIO_OWNER', 'MANAGER')")
    public ResponseEntity<ApiResponse<List<CampaignRecipientDto>>> getRecipients(
            @PathVariable UUID id,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        Page<CampaignRecipientDto> result = campaignService.getRecipients(id, PageRequest.of(page, size));
        return ResponseEntity.ok(ApiResponse.success(result.getContent(), PageMeta.from(result)));
    }

    @GetMapping("/stats")
    @PreAuthorize("hasAnyRole('STUDIO_OWNER', 'MANAGER')")
    public ResponseEntity<ApiResponse<CampaignStatsDto>> getStats() {
        return ResponseEntity.ok(ApiResponse.success(campaignService.getOverallStats()));
    }

    @PostMapping("/audience/preview")
    @PreAuthorize("hasAnyRole('STUDIO_OWNER', 'MANAGER')")
    public ResponseEntity<ApiResponse<AudiencePreviewDto>> previewAudience(
            @RequestBody AudienceCriteria criteria) {
        return ResponseEntity.ok(ApiResponse.success(audienceBuilderService.preview(criteria)));
    }
}

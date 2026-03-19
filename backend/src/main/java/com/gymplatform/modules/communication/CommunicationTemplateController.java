package com.gymplatform.modules.communication;

import com.gymplatform.modules.communication.dto.*;
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
@RequestMapping("/api/communication/templates")
@RequiredArgsConstructor
public class CommunicationTemplateController {

    private final CommunicationTemplateService templateService;

    @GetMapping
    @PreAuthorize("hasAnyRole('STUDIO_OWNER', 'MANAGER')")
    public ResponseEntity<ApiResponse<List<CommunicationTemplateDto>>> getAll(
            @RequestParam(required = false) ChannelType channelType,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        Page<CommunicationTemplateDto> result = channelType != null
                ? templateService.getByChannel(channelType, PageRequest.of(page, size))
                : templateService.getAll(PageRequest.of(page, size));
        return ResponseEntity.ok(ApiResponse.success(result.getContent(), PageMeta.from(result)));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('STUDIO_OWNER', 'MANAGER')")
    public ResponseEntity<ApiResponse<CommunicationTemplateDto>> getById(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.success(templateService.getById(id)));
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('STUDIO_OWNER', 'MANAGER')")
    public ResponseEntity<ApiResponse<CommunicationTemplateDto>> create(
            @Valid @RequestBody CreateTemplateRequest req,
            @AuthenticationPrincipal UserDetails user) {
        UUID userId = UUID.fromString(user.getUsername());
        return ResponseEntity.ok(ApiResponse.success(templateService.create(req, userId)));
    }

    @PostMapping("/{id}")
    @PreAuthorize("hasAnyRole('STUDIO_OWNER', 'MANAGER')")
    public ResponseEntity<ApiResponse<CommunicationTemplateDto>> update(
            @PathVariable UUID id,
            @Valid @RequestBody CreateTemplateRequest req,
            @AuthenticationPrincipal UserDetails user) {
        UUID userId = UUID.fromString(user.getUsername());
        return ResponseEntity.ok(ApiResponse.success(templateService.update(id, req, userId)));
    }

    @PostMapping("/{id}/deactivate")
    @PreAuthorize("hasAnyRole('STUDIO_OWNER', 'MANAGER')")
    public ResponseEntity<ApiResponse<Void>> deactivate(
            @PathVariable UUID id,
            @AuthenticationPrincipal UserDetails user) {
        UUID userId = UUID.fromString(user.getUsername());
        templateService.deactivate(id, userId);
        return ResponseEntity.ok(ApiResponse.success(null));
    }

    @PostMapping("/preview")
    @PreAuthorize("hasAnyRole('STUDIO_OWNER', 'MANAGER')")
    public ResponseEntity<ApiResponse<TemplatePreviewDto>> preview(
            @RequestBody PreviewTemplateRequest req) {
        return ResponseEntity.ok(ApiResponse.success(
                templateService.preview(req.getTemplateId(), req.getVariables())));
    }
}

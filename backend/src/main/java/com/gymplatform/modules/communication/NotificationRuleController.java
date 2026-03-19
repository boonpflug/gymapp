package com.gymplatform.modules.communication;

import com.gymplatform.modules.communication.dto.CreateNotificationRuleRequest;
import com.gymplatform.modules.communication.dto.NotificationRuleDto;
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
@RequestMapping("/api/communication/rules")
@RequiredArgsConstructor
public class NotificationRuleController {

    private final NotificationRuleService ruleService;

    @GetMapping
    @PreAuthorize("hasAnyRole('STUDIO_OWNER', 'MANAGER')")
    public ResponseEntity<ApiResponse<List<NotificationRuleDto>>> getAll() {
        return ResponseEntity.ok(ApiResponse.success(ruleService.getAll()));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('STUDIO_OWNER', 'MANAGER')")
    public ResponseEntity<ApiResponse<NotificationRuleDto>> getById(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.success(ruleService.getById(id)));
    }

    @GetMapping("/trigger/{triggerEvent}")
    @PreAuthorize("hasAnyRole('STUDIO_OWNER', 'MANAGER')")
    public ResponseEntity<ApiResponse<List<NotificationRuleDto>>> getByTrigger(
            @PathVariable TriggerEvent triggerEvent) {
        return ResponseEntity.ok(ApiResponse.success(ruleService.getByTrigger(triggerEvent)));
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('STUDIO_OWNER', 'MANAGER')")
    public ResponseEntity<ApiResponse<NotificationRuleDto>> create(
            @Valid @RequestBody CreateNotificationRuleRequest req,
            @AuthenticationPrincipal UserDetails user) {
        UUID userId = UUID.fromString(user.getUsername());
        return ResponseEntity.ok(ApiResponse.success(ruleService.create(req, userId)));
    }

    @PostMapping("/{id}")
    @PreAuthorize("hasAnyRole('STUDIO_OWNER', 'MANAGER')")
    public ResponseEntity<ApiResponse<NotificationRuleDto>> update(
            @PathVariable UUID id,
            @Valid @RequestBody CreateNotificationRuleRequest req,
            @AuthenticationPrincipal UserDetails user) {
        UUID userId = UUID.fromString(user.getUsername());
        return ResponseEntity.ok(ApiResponse.success(ruleService.update(id, req, userId)));
    }

    @PostMapping("/{id}/toggle")
    @PreAuthorize("hasAnyRole('STUDIO_OWNER', 'MANAGER')")
    public ResponseEntity<ApiResponse<Void>> toggle(
            @PathVariable UUID id,
            @AuthenticationPrincipal UserDetails user) {
        UUID userId = UUID.fromString(user.getUsername());
        ruleService.toggleActive(id, userId);
        return ResponseEntity.ok(ApiResponse.success(null));
    }

    @PostMapping("/{id}/delete")
    @PreAuthorize("hasAnyRole('STUDIO_OWNER', 'MANAGER')")
    public ResponseEntity<ApiResponse<Void>> delete(
            @PathVariable UUID id,
            @AuthenticationPrincipal UserDetails user) {
        UUID userId = UUID.fromString(user.getUsername());
        ruleService.delete(id, userId);
        return ResponseEntity.ok(ApiResponse.success(null));
    }
}

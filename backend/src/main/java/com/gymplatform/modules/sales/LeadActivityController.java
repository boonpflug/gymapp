package com.gymplatform.modules.sales;

import com.gymplatform.modules.sales.dto.CreateLeadActivityRequest;
import com.gymplatform.modules.sales.dto.LeadActivityDto;
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
@RequestMapping("/api/sales/activities")
@RequiredArgsConstructor
public class LeadActivityController {

    private final LeadActivityService activityService;

    @PostMapping
    @PreAuthorize("hasAnyRole('STUDIO_OWNER', 'MANAGER', 'RECEPTIONIST')")
    public ResponseEntity<ApiResponse<LeadActivityDto>> create(
            @Valid @RequestBody CreateLeadActivityRequest req,
            @AuthenticationPrincipal UserDetails user) {
        UUID userId = UUID.fromString(user.getUsername());
        return ResponseEntity.ok(ApiResponse.success(activityService.create(req, userId)));
    }

    @PostMapping("/{id}/complete")
    @PreAuthorize("hasAnyRole('STUDIO_OWNER', 'MANAGER', 'RECEPTIONIST')")
    public ResponseEntity<ApiResponse<LeadActivityDto>> complete(
            @PathVariable UUID id,
            @RequestParam(required = false) String outcome,
            @AuthenticationPrincipal UserDetails user) {
        UUID userId = UUID.fromString(user.getUsername());
        return ResponseEntity.ok(ApiResponse.success(activityService.completeTask(id, outcome, userId)));
    }

    @GetMapping("/lead/{leadId}")
    @PreAuthorize("hasAnyRole('STUDIO_OWNER', 'MANAGER', 'RECEPTIONIST')")
    public ResponseEntity<ApiResponse<List<LeadActivityDto>>> getByLead(@PathVariable UUID leadId) {
        return ResponseEntity.ok(ApiResponse.success(activityService.getByLead(leadId)));
    }

    @GetMapping("/overdue")
    @PreAuthorize("hasAnyRole('STUDIO_OWNER', 'MANAGER')")
    public ResponseEntity<ApiResponse<List<LeadActivityDto>>> getOverdue() {
        return ResponseEntity.ok(ApiResponse.success(activityService.getOverdueTasks()));
    }

    @GetMapping("/my-tasks")
    @PreAuthorize("hasAnyRole('STUDIO_OWNER', 'MANAGER', 'RECEPTIONIST')")
    public ResponseEntity<ApiResponse<List<LeadActivityDto>>> getMyTasks(
            @AuthenticationPrincipal UserDetails user) {
        UUID userId = UUID.fromString(user.getUsername());
        return ResponseEntity.ok(ApiResponse.success(activityService.getPendingTasksByStaff(userId)));
    }
}

package com.gymplatform.modules.training;

import com.gymplatform.modules.training.dto.CreateTrainingPlanRequest;
import com.gymplatform.modules.training.dto.ReorderExercisesRequest;
import com.gymplatform.modules.training.dto.TrainingPlanDto;
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
@RequestMapping("/api/training/plans")
@RequiredArgsConstructor
public class TrainingPlanController {

    private final TrainingPlanService planService;

    @PostMapping
    @PreAuthorize("hasAnyRole('STUDIO_OWNER', 'MANAGER', 'TRAINER')")
    public ResponseEntity<ApiResponse<TrainingPlanDto>> create(
            @Valid @RequestBody CreateTrainingPlanRequest req,
            @AuthenticationPrincipal UserDetails user) {
        UUID trainerId = UUID.fromString(user.getUsername());
        return ResponseEntity.ok(ApiResponse.success(planService.create(req, trainerId)));
    }

    @PostMapping("/{id}")
    @PreAuthorize("hasAnyRole('STUDIO_OWNER', 'MANAGER', 'TRAINER')")
    public ResponseEntity<ApiResponse<TrainingPlanDto>> update(
            @PathVariable UUID id,
            @Valid @RequestBody CreateTrainingPlanRequest req,
            @AuthenticationPrincipal UserDetails user) {
        UUID userId = UUID.fromString(user.getUsername());
        return ResponseEntity.ok(ApiResponse.success(planService.update(id, req, userId)));
    }

    @PostMapping("/{id}/publish")
    @PreAuthorize("hasAnyRole('STUDIO_OWNER', 'MANAGER', 'TRAINER')")
    public ResponseEntity<ApiResponse<TrainingPlanDto>> publish(
            @PathVariable UUID id,
            @AuthenticationPrincipal UserDetails user) {
        UUID userId = UUID.fromString(user.getUsername());
        return ResponseEntity.ok(ApiResponse.success(planService.publish(id, userId)));
    }

    @PostMapping("/{id}/archive")
    @PreAuthorize("hasAnyRole('STUDIO_OWNER', 'MANAGER', 'TRAINER')")
    public ResponseEntity<ApiResponse<TrainingPlanDto>> archive(
            @PathVariable UUID id,
            @AuthenticationPrincipal UserDetails user) {
        UUID userId = UUID.fromString(user.getUsername());
        return ResponseEntity.ok(ApiResponse.success(planService.archive(id, userId)));
    }

    @PostMapping("/from-template/{templateId}")
    @PreAuthorize("hasAnyRole('STUDIO_OWNER', 'MANAGER', 'TRAINER')")
    public ResponseEntity<ApiResponse<TrainingPlanDto>> createFromTemplate(
            @PathVariable UUID templateId,
            @RequestParam UUID memberId,
            @AuthenticationPrincipal UserDetails user) {
        UUID trainerId = UUID.fromString(user.getUsername());
        return ResponseEntity.ok(ApiResponse.success(
                planService.createFromTemplate(templateId, memberId, trainerId)));
    }

    @PostMapping("/{id}/reorder")
    @PreAuthorize("hasAnyRole('STUDIO_OWNER', 'MANAGER', 'TRAINER')")
    public ResponseEntity<ApiResponse<Void>> reorderExercises(
            @PathVariable UUID id,
            @Valid @RequestBody ReorderExercisesRequest req,
            @AuthenticationPrincipal UserDetails user) {
        UUID userId = UUID.fromString(user.getUsername());
        planService.reorderExercises(id, req.getExerciseIds(), userId);
        return ResponseEntity.ok(ApiResponse.success(null));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('STUDIO_OWNER', 'MANAGER', 'TRAINER', 'MEMBER')")
    public ResponseEntity<ApiResponse<TrainingPlanDto>> getById(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.success(planService.getById(id)));
    }

    @GetMapping("/member/{memberId}")
    @PreAuthorize("hasAnyRole('STUDIO_OWNER', 'MANAGER', 'TRAINER', 'MEMBER')")
    public ResponseEntity<ApiResponse<List<TrainingPlanDto>>> getMemberPlans(
            @PathVariable UUID memberId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        Page<TrainingPlanDto> result = planService.getMemberPlans(memberId, PageRequest.of(page, size));
        return ResponseEntity.ok(ApiResponse.success(result.getContent(), PageMeta.from(result)));
    }

    @GetMapping("/templates")
    @PreAuthorize("hasAnyRole('STUDIO_OWNER', 'MANAGER', 'TRAINER')")
    public ResponseEntity<ApiResponse<List<TrainingPlanDto>>> getTemplates(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        Page<TrainingPlanDto> result = planService.getTemplates(PageRequest.of(page, size));
        return ResponseEntity.ok(ApiResponse.success(result.getContent(), PageMeta.from(result)));
    }

    @GetMapping("/catalog")
    @PreAuthorize("hasAnyRole('STUDIO_OWNER', 'MANAGER', 'TRAINER', 'MEMBER')")
    public ResponseEntity<ApiResponse<List<TrainingPlanDto>>> getCatalog(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        Page<TrainingPlanDto> result = planService.getCatalog(PageRequest.of(page, size));
        return ResponseEntity.ok(ApiResponse.success(result.getContent(), PageMeta.from(result)));
    }
}

package com.gymplatform.modules.training;

import com.gymplatform.modules.training.dto.CreateExerciseRequest;
import com.gymplatform.modules.training.dto.ExerciseDto;
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
@RequestMapping("/api/training/exercises")
@RequiredArgsConstructor
public class ExerciseController {

    private final ExerciseService exerciseService;

    @GetMapping
    @PreAuthorize("hasAnyRole('STUDIO_OWNER', 'MANAGER', 'TRAINER', 'MEMBER')")
    public ResponseEntity<ApiResponse<List<ExerciseDto>>> searchExercises(
            @RequestParam(required = false) String name,
            @RequestParam(required = false) MuscleGroup muscleGroup,
            @RequestParam(required = false) ExerciseType exerciseType,
            @RequestParam(required = false) String equipment,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        Page<ExerciseDto> result = exerciseService.searchExercises(
                name, muscleGroup, exerciseType, equipment, PageRequest.of(page, size));
        return ResponseEntity.ok(ApiResponse.success(result.getContent(), PageMeta.from(result)));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('STUDIO_OWNER', 'MANAGER', 'TRAINER', 'MEMBER')")
    public ResponseEntity<ApiResponse<ExerciseDto>> getById(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.success(exerciseService.getById(id)));
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('STUDIO_OWNER', 'MANAGER', 'TRAINER')")
    public ResponseEntity<ApiResponse<ExerciseDto>> create(
            @Valid @RequestBody CreateExerciseRequest req,
            @AuthenticationPrincipal UserDetails user) {
        UUID userId = UUID.fromString(user.getUsername());
        return ResponseEntity.ok(ApiResponse.success(exerciseService.create(req, userId)));
    }

    @PostMapping("/{id}")
    @PreAuthorize("hasAnyRole('STUDIO_OWNER', 'MANAGER', 'TRAINER')")
    public ResponseEntity<ApiResponse<ExerciseDto>> update(
            @PathVariable UUID id,
            @Valid @RequestBody CreateExerciseRequest req,
            @AuthenticationPrincipal UserDetails user) {
        UUID userId = UUID.fromString(user.getUsername());
        return ResponseEntity.ok(ApiResponse.success(exerciseService.update(id, req, userId)));
    }

    @PostMapping("/{id}/deactivate")
    @PreAuthorize("hasAnyRole('STUDIO_OWNER', 'MANAGER')")
    public ResponseEntity<ApiResponse<Void>> deactivate(
            @PathVariable UUID id,
            @AuthenticationPrincipal UserDetails user) {
        UUID userId = UUID.fromString(user.getUsername());
        exerciseService.deactivate(id, userId);
        return ResponseEntity.ok(ApiResponse.success(null));
    }

    @GetMapping("/equipment")
    @PreAuthorize("hasAnyRole('STUDIO_OWNER', 'MANAGER', 'TRAINER', 'MEMBER')")
    public ResponseEntity<ApiResponse<List<String>>> getEquipment() {
        return ResponseEntity.ok(ApiResponse.success(exerciseService.getEquipmentList()));
    }
}

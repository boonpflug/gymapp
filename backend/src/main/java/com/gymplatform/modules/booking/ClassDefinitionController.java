package com.gymplatform.modules.booking;

import com.gymplatform.modules.booking.dto.ClassDefinitionDto;
import com.gymplatform.modules.booking.dto.CreateClassRequest;
import com.gymplatform.shared.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/booking/classes")
@RequiredArgsConstructor
public class ClassDefinitionController {

    private final ClassDefinitionService classService;

    @GetMapping
    @PreAuthorize("hasAnyRole('STUDIO_OWNER', 'MANAGER', 'TRAINER', 'RECEPTIONIST', 'MEMBER')")
    public ResponseEntity<ApiResponse<Page<ClassDefinitionDto>>> getAll(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(ApiResponse.success(classService.getAll(PageRequest.of(page, size))));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('STUDIO_OWNER', 'MANAGER', 'TRAINER', 'RECEPTIONIST', 'MEMBER')")
    public ResponseEntity<ApiResponse<ClassDefinitionDto>> getById(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.success(classService.getById(id)));
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('STUDIO_OWNER', 'MANAGER')")
    public ResponseEntity<ApiResponse<ClassDefinitionDto>> create(
            @Valid @RequestBody CreateClassRequest req) {
        return ResponseEntity.ok(ApiResponse.success(classService.create(req)));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('STUDIO_OWNER', 'MANAGER')")
    public ResponseEntity<ApiResponse<ClassDefinitionDto>> update(
            @PathVariable UUID id,
            @Valid @RequestBody CreateClassRequest req) {
        return ResponseEntity.ok(ApiResponse.success(classService.update(id, req)));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('STUDIO_OWNER', 'MANAGER')")
    public ResponseEntity<ApiResponse<Void>> deactivate(@PathVariable UUID id) {
        classService.deactivate(id);
        return ResponseEntity.ok(ApiResponse.success(null));
    }
}

package com.gymplatform.modules.booking;

import com.gymplatform.modules.booking.dto.ClassCategoryDto;
import com.gymplatform.modules.booking.dto.CreateClassCategoryRequest;
import com.gymplatform.shared.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/booking/categories")
@RequiredArgsConstructor
public class ClassCategoryController {

    private final ClassCategoryService categoryService;

    @GetMapping
    @PreAuthorize("hasAnyRole('STUDIO_OWNER', 'MANAGER', 'TRAINER', 'RECEPTIONIST', 'MEMBER')")
    public ResponseEntity<ApiResponse<List<ClassCategoryDto>>> getAll() {
        return ResponseEntity.ok(ApiResponse.success(categoryService.getAllActive()));
    }

    @GetMapping("/all")
    @PreAuthorize("hasAnyRole('STUDIO_OWNER', 'MANAGER')")
    public ResponseEntity<ApiResponse<List<ClassCategoryDto>>> getAllIncludingInactive() {
        return ResponseEntity.ok(ApiResponse.success(categoryService.getAll()));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('STUDIO_OWNER', 'MANAGER', 'TRAINER', 'RECEPTIONIST', 'MEMBER')")
    public ResponseEntity<ApiResponse<ClassCategoryDto>> getById(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.success(categoryService.getById(id)));
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('STUDIO_OWNER', 'MANAGER')")
    public ResponseEntity<ApiResponse<ClassCategoryDto>> create(
            @Valid @RequestBody CreateClassCategoryRequest req) {
        return ResponseEntity.ok(ApiResponse.success(categoryService.create(req)));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('STUDIO_OWNER', 'MANAGER')")
    public ResponseEntity<ApiResponse<ClassCategoryDto>> update(
            @PathVariable UUID id,
            @Valid @RequestBody CreateClassCategoryRequest req) {
        return ResponseEntity.ok(ApiResponse.success(categoryService.update(id, req)));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('STUDIO_OWNER', 'MANAGER')")
    public ResponseEntity<ApiResponse<Void>> deactivate(@PathVariable UUID id) {
        categoryService.deactivate(id);
        return ResponseEntity.ok(ApiResponse.success(null));
    }
}

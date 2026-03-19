package com.gymplatform.modules.staff;

import com.gymplatform.modules.staff.dto.CreateEmployeeRequest;
import com.gymplatform.modules.staff.dto.EmployeeDto;
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
@RequestMapping("/api/staff/employees")
@RequiredArgsConstructor
public class EmployeeController {

    private final EmployeeService employeeService;

    @GetMapping
    @PreAuthorize("hasAnyRole('STUDIO_OWNER', 'MANAGER')")
    public ResponseEntity<ApiResponse<List<EmployeeDto>>> getAll(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        Page<EmployeeDto> result = employeeService.getAll(PageRequest.of(page, size));
        return ResponseEntity.ok(ApiResponse.success(result.getContent(), PageMeta.from(result)));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('STUDIO_OWNER', 'MANAGER')")
    public ResponseEntity<ApiResponse<EmployeeDto>> getById(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.success(employeeService.getById(id)));
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('STUDIO_OWNER', 'MANAGER')")
    public ResponseEntity<ApiResponse<EmployeeDto>> create(
            @Valid @RequestBody CreateEmployeeRequest req,
            @AuthenticationPrincipal UserDetails user) {
        UUID userId = UUID.fromString(user.getUsername());
        return ResponseEntity.ok(ApiResponse.success(employeeService.create(req, userId)));
    }

    @PostMapping("/{id}")
    @PreAuthorize("hasAnyRole('STUDIO_OWNER', 'MANAGER')")
    public ResponseEntity<ApiResponse<EmployeeDto>> update(
            @PathVariable UUID id,
            @Valid @RequestBody CreateEmployeeRequest req,
            @AuthenticationPrincipal UserDetails user) {
        UUID userId = UUID.fromString(user.getUsername());
        return ResponseEntity.ok(ApiResponse.success(employeeService.update(id, req, userId)));
    }

    @PostMapping("/{id}/deactivate")
    @PreAuthorize("hasAnyRole('STUDIO_OWNER', 'MANAGER')")
    public ResponseEntity<ApiResponse<Void>> deactivate(
            @PathVariable UUID id,
            @AuthenticationPrincipal UserDetails user) {
        UUID userId = UUID.fromString(user.getUsername());
        employeeService.deactivate(id, userId);
        return ResponseEntity.ok(ApiResponse.success(null));
    }
}

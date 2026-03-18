package com.gymplatform.modules.tenant;

import com.gymplatform.modules.tenant.dto.CreateTenantRequest;
import com.gymplatform.modules.tenant.dto.TenantDto;
import com.gymplatform.shared.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/superadmin/tenants")
@PreAuthorize("hasRole('SUPER_ADMIN')")
@RequiredArgsConstructor
public class TenantController {

    private final TenantService tenantService;

    @PostMapping
    public ResponseEntity<ApiResponse<TenantDto>> createTenant(
            @Valid @RequestBody CreateTenantRequest request) {
        TenantDto tenant = tenantService.createTenant(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(tenant));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<TenantDto>>> getAllTenants() {
        return ResponseEntity.ok(ApiResponse.success(tenantService.getAllTenants()));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<TenantDto>> getTenant(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.success(tenantService.getTenant(id)));
    }
}

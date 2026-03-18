package com.gymplatform.modules.tenant;

import com.gymplatform.config.multitenancy.TenantProvisioningService;
import com.gymplatform.modules.tenant.dto.CreateTenantRequest;
import com.gymplatform.modules.tenant.dto.TenantDto;
import com.gymplatform.shared.BusinessException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class TenantService {

    private final TenantRepository tenantRepository;
    private final TenantProvisioningService provisioningService;

    @Transactional
    public TenantDto createTenant(CreateTenantRequest request) {
        if (tenantRepository.existsBySubdomain(request.getSubdomain())) {
            throw BusinessException.conflict("Subdomain already taken: " + request.getSubdomain());
        }

        String schemaName = "tenant_" + request.getSubdomain().replace("-", "_");

        Tenant tenant = Tenant.builder()
                .name(request.getName())
                .subdomain(request.getSubdomain())
                .schemaName(schemaName)
                .status(Tenant.TenantStatus.TRIAL)
                .planTier("STARTER")
                .ownerEmail(request.getOwnerEmail())
                .trialEndsAt(Instant.now().plus(30, ChronoUnit.DAYS))
                .build();

        tenant = tenantRepository.save(tenant);

        provisioningService.provisionTenant(request.getSubdomain());

        log.info("Tenant created: {} ({})", tenant.getName(), tenant.getSchemaName());
        return toDto(tenant);
    }

    public TenantDto getTenant(UUID id) {
        return toDto(tenantRepository.findById(id)
                .orElseThrow(() -> BusinessException.notFound("Tenant", id)));
    }

    public List<TenantDto> getAllTenants() {
        return tenantRepository.findAll().stream().map(this::toDto).toList();
    }

    private TenantDto toDto(Tenant tenant) {
        return TenantDto.builder()
                .id(tenant.getId())
                .name(tenant.getName())
                .subdomain(tenant.getSubdomain())
                .status(tenant.getStatus().name())
                .planTier(tenant.getPlanTier())
                .ownerEmail(tenant.getOwnerEmail())
                .createdAt(tenant.getCreatedAt())
                .build();
    }
}

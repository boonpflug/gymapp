package com.gymplatform.modules.tenant;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface TenantRepository extends JpaRepository<Tenant, UUID> {
    Optional<Tenant> findBySubdomain(String subdomain);
    Optional<Tenant> findBySchemaName(String schemaName);
    boolean existsBySubdomain(String subdomain);
}

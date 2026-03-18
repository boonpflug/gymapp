package com.gymplatform.modules.tenant.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TenantDto {
    private UUID id;
    private String name;
    private String subdomain;
    private String status;
    private String planTier;
    private String ownerEmail;
    private Instant createdAt;
}

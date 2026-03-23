package com.gymplatform.modules.auth.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class SsoLoginRequest {
    @NotBlank
    private String idToken;
    private String tenantId;  // X-Tenant-ID header is also used
}

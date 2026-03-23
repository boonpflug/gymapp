package com.gymplatform.modules.auth;

import com.gymplatform.config.multitenancy.TenantContext;
import com.gymplatform.modules.auth.dto.AuthResponse;
import com.gymplatform.modules.auth.dto.UserDto;
import com.gymplatform.shared.BusinessException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Base64;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.databind.ObjectMapper;

@Service
@Slf4j
public class SsoService {

    private final UserRepository userRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final JwtService jwtService;
    private final ObjectMapper objectMapper;

    private final boolean azureEnabled;
    private final String azureClientId;
    private final String azureTenantId;
    private final String defaultRole;

    public SsoService(
            UserRepository userRepository,
            RefreshTokenRepository refreshTokenRepository,
            JwtService jwtService,
            ObjectMapper objectMapper,
            @Value("${sso.azure.enabled:false}") boolean azureEnabled,
            @Value("${sso.azure.client-id:}") String azureClientId,
            @Value("${sso.azure.tenant-id:}") String azureTenantId,
            @Value("${sso.azure.default-role:MEMBER}") String defaultRole) {
        this.userRepository = userRepository;
        this.refreshTokenRepository = refreshTokenRepository;
        this.jwtService = jwtService;
        this.objectMapper = objectMapper;
        this.azureEnabled = azureEnabled;
        this.azureClientId = azureClientId;
        this.azureTenantId = azureTenantId;
        this.defaultRole = defaultRole;
    }

    /**
     * Authenticate a user via Azure AD SSO.
     * Validates the ID token, finds or creates the user, and returns JWT tokens.
     */
    @Transactional
    public AuthResponse authenticateWithAzure(String idToken, String tenantId) {
        if (!azureEnabled) {
            throw new BusinessException("SSO_DISABLED",
                    "Azure AD SSO is not enabled", HttpStatus.BAD_REQUEST);
        }

        Map<String, Object> claims = validateAzureToken(idToken);

        String oid = (String) claims.get("oid");
        String email = extractEmail(claims);
        String firstName = (String) claims.getOrDefault("given_name", "");
        String lastName = (String) claims.getOrDefault("family_name", "");

        if (oid == null || oid.isBlank()) {
            throw new BusinessException("INVALID_SSO_TOKEN",
                    "Azure AD token missing object ID (oid)", HttpStatus.BAD_REQUEST);
        }
        if (email == null || email.isBlank()) {
            throw new BusinessException("INVALID_SSO_TOKEN",
                    "Azure AD token missing email claim", HttpStatus.BAD_REQUEST);
        }

        String effectiveTenantId = tenantId != null ? tenantId : TenantContext.getTenantId();

        // 1. Check if user with this SSO subject ID already exists
        User user = userRepository.findBySsoSubjectId(oid).orElse(null);

        if (user != null) {
            // Existing SSO user — just login
            user.setLastLoginAt(Instant.now());
            userRepository.save(user);
            return createAuthResponse(user);
        }

        // 2. Check if user with matching email exists (link SSO to existing account)
        user = userRepository.findByEmailAndTenantId(email, effectiveTenantId)
                .or(() -> userRepository.findByEmail(email))
                .orElse(null);

        if (user != null) {
            // Link SSO to existing account
            user.setSsoProvider("azure");
            user.setSsoSubjectId(oid);
            user.setLastLoginAt(Instant.now());
            userRepository.save(user);
            log.info("Linked Azure AD SSO to existing user: {} ({})", user.getEmail(), user.getId());
            return createAuthResponse(user);
        }

        // 3. Auto-create new user
        Role role = mapAzureRoleToAppRole(claims);

        user = User.builder()
                .email(email)
                .firstName(firstName.isEmpty() ? email.split("@")[0] : firstName)
                .lastName(lastName.isEmpty() ? "" : lastName)
                .passwordHash(null)
                .role(role)
                .tenantId(effectiveTenantId)
                .enabled(true)
                .emailVerified(true)  // Azure AD has already verified the email
                .ssoProvider("azure")
                .ssoSubjectId(oid)
                .lastLoginAt(Instant.now())
                .build();

        user = userRepository.save(user);
        log.info("Auto-created user via Azure AD SSO: {} ({})", user.getEmail(), user.getId());

        return createAuthResponse(user);
    }

    /**
     * Validate an Azure AD ID token (JWT).
     * Decodes the JWT, verifies issuer, audience, and expiry.
     *
     * TODO: For full production security, validate the token signature using the
     * Azure AD JWKS endpoint (https://login.microsoftonline.com/{tenant}/discovery/v2.0/keys).
     * Currently this performs issuer, audience, and expiry checks only.
     */
    @SuppressWarnings("unchecked")
    Map<String, Object> validateAzureToken(String idToken) {
        try {
            String[] parts = idToken.split("\\.");
            if (parts.length != 3) {
                throw new BusinessException("INVALID_SSO_TOKEN",
                        "Invalid JWT format", HttpStatus.BAD_REQUEST);
            }

            // Decode payload (part[1])
            String payloadJson = new String(Base64.getUrlDecoder().decode(parts[1]));
            Map<String, Object> claims = objectMapper.readValue(payloadJson, Map.class);

            // Verify issuer
            String issuer = (String) claims.get("iss");
            String expectedIssuer = "https://login.microsoftonline.com/" + azureTenantId + "/v2.0";
            if (issuer == null || !issuer.equals(expectedIssuer)) {
                throw new BusinessException("INVALID_SSO_TOKEN",
                        "Invalid token issuer. Expected: " + expectedIssuer + ", got: " + issuer,
                        HttpStatus.UNAUTHORIZED);
            }

            // Verify audience
            String audience = (String) claims.get("aud");
            if (audience == null || !audience.equals(azureClientId)) {
                throw new BusinessException("INVALID_SSO_TOKEN",
                        "Invalid token audience", HttpStatus.UNAUTHORIZED);
            }

            // Verify expiry
            Object expObj = claims.get("exp");
            if (expObj != null) {
                long exp;
                if (expObj instanceof Number) {
                    exp = ((Number) expObj).longValue();
                } else {
                    exp = Long.parseLong(expObj.toString());
                }
                if (Instant.ofEpochSecond(exp).isBefore(Instant.now())) {
                    throw new BusinessException("INVALID_SSO_TOKEN",
                            "Token has expired", HttpStatus.UNAUTHORIZED);
                }
            }

            return claims;
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("Failed to validate Azure AD token", e);
            throw new BusinessException("INVALID_SSO_TOKEN",
                    "Failed to validate Azure AD token: " + e.getMessage(),
                    HttpStatus.UNAUTHORIZED);
        }
    }

    /**
     * Maps Azure AD group memberships or roles to application roles.
     * Uses the configured default role if no specific mapping is found.
     */
    @SuppressWarnings("unchecked")
    Role mapAzureRoleToAppRole(Map<String, Object> claims) {
        // Check for roles claim (Azure AD app roles)
        Object rolesObj = claims.get("roles");
        if (rolesObj instanceof List<?> roles && !roles.isEmpty()) {
            for (Object role : roles) {
                String roleStr = role.toString().toUpperCase();
                try {
                    return Role.valueOf(roleStr);
                } catch (IllegalArgumentException ignored) {
                    // Role doesn't map to an app role, try next
                }
            }
        }

        // Check for groups claim
        Object groupsObj = claims.get("groups");
        if (groupsObj instanceof List<?> groups && !groups.isEmpty()) {
            for (Object group : groups) {
                String groupStr = group.toString().toUpperCase();
                try {
                    return Role.valueOf(groupStr);
                } catch (IllegalArgumentException ignored) {
                    // Group doesn't map to an app role, try next
                }
            }
        }

        // Fall back to configured default role
        try {
            return Role.valueOf(defaultRole);
        } catch (IllegalArgumentException e) {
            return Role.MEMBER;
        }
    }

    private String extractEmail(Map<String, Object> claims) {
        // Azure AD may put email in different claims
        String email = (String) claims.get("email");
        if (email == null || email.isBlank()) {
            email = (String) claims.get("preferred_username");
        }
        if (email == null || email.isBlank()) {
            email = (String) claims.get("upn");
        }
        return email;
    }

    private AuthResponse createAuthResponse(User user) {
        String accessToken = jwtService.generateAccessToken(user);

        RefreshToken refreshToken = RefreshToken.builder()
                .token(jwtService.generateRefreshTokenValue())
                .userId(user.getId())
                .expiresAt(Instant.now().plusMillis(jwtService.getRefreshTokenExpiry()))
                .revoked(false)
                .createdAt(Instant.now())
                .build();
        refreshTokenRepository.save(refreshToken);

        return AuthResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken.getToken())
                .expiresIn(900)
                .user(UserDto.builder()
                        .id(user.getId())
                        .email(user.getEmail())
                        .firstName(user.getFirstName())
                        .lastName(user.getLastName())
                        .role(user.getRole())
                        .build())
                .build();
    }
}

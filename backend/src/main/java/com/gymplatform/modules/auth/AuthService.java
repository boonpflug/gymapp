package com.gymplatform.modules.auth;

import com.gymplatform.config.multitenancy.TenantContext;
import com.gymplatform.modules.auth.dto.*;
import com.gymplatform.shared.BusinessException;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final JwtService jwtService;
    private final PasswordEncoder passwordEncoder;

    @Transactional
    public AuthResponse register(RegisterRequest request) {
        String tenantId = request.getTenantId() != null ? request.getTenantId() : TenantContext.getTenantId();

        if (userRepository.existsByEmailAndTenantId(request.getEmail(), tenantId)) {
            throw new BusinessException("EMAIL_EXISTS",
                    "Email already registered", HttpStatus.CONFLICT);
        }

        User user = User.builder()
                .email(request.getEmail())
                .passwordHash(passwordEncoder.encode(request.getPassword()))
                .firstName(request.getFirstName())
                .lastName(request.getLastName())
                .role(request.getRole() != null ? request.getRole() : Role.MEMBER)
                .tenantId(tenantId)
                .enabled(true)
                .emailVerified(false)
                .build();

        user = userRepository.save(user);
        return createAuthResponse(user);
    }

    @Transactional
    public AuthResponse login(LoginRequest request) {
        String tenantId = TenantContext.getTenantId();

        User user = userRepository.findByEmailAndTenantId(request.getEmail(), tenantId)
                .or(() -> userRepository.findByEmail(request.getEmail()))
                .orElseThrow(() -> new BusinessException("INVALID_CREDENTIALS",
                        "Invalid email or password", HttpStatus.UNAUTHORIZED));

        if (user.getPasswordHash() == null || user.getPasswordHash().isBlank()) {
            throw new BusinessException("SSO_ONLY_ACCOUNT",
                    "This account uses SSO login. Please sign in with your SSO provider.",
                    HttpStatus.BAD_REQUEST);
        }

        if (!passwordEncoder.matches(request.getPassword(), user.getPasswordHash())) {
            throw new BusinessException("INVALID_CREDENTIALS",
                    "Invalid email or password", HttpStatus.UNAUTHORIZED);
        }

        if (!user.isEnabled()) {
            throw new BusinessException("ACCOUNT_DISABLED",
                    "Account is disabled", HttpStatus.FORBIDDEN);
        }

        user.setLastLoginAt(Instant.now());
        userRepository.save(user);

        return createAuthResponse(user);
    }

    @Transactional
    public AuthResponse refreshToken(RefreshTokenRequest request) {
        RefreshToken refreshToken = refreshTokenRepository.findByToken(request.getRefreshToken())
                .orElseThrow(() -> new BusinessException("INVALID_TOKEN",
                        "Invalid refresh token", HttpStatus.UNAUTHORIZED));

        if (refreshToken.isRevoked() || refreshToken.getExpiresAt().isBefore(Instant.now())) {
            throw new BusinessException("TOKEN_EXPIRED",
                    "Refresh token expired", HttpStatus.UNAUTHORIZED);
        }

        refreshToken.setRevoked(true);
        refreshTokenRepository.save(refreshToken);

        User user = userRepository.findById(refreshToken.getUserId())
                .orElseThrow(() -> BusinessException.notFound("User", refreshToken.getUserId()));

        return createAuthResponse(user);
    }

    @Transactional
    public void logout(String refreshTokenValue) {
        refreshTokenRepository.findByToken(refreshTokenValue)
                .ifPresent(token -> {
                    token.setRevoked(true);
                    refreshTokenRepository.save(token);
                });
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

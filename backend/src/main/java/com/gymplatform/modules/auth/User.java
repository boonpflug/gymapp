package com.gymplatform.modules.auth;

import com.gymplatform.shared.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

@Entity
@Table(name = "users")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class User extends BaseEntity {

    @Column(nullable = false)
    private String email;

    @Column(name = "password_hash")
    private String passwordHash;

    @Column(name = "first_name", nullable = false)
    private String firstName;

    @Column(name = "last_name", nullable = false)
    private String lastName;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Role role;

    @Column(name = "tenant_id")
    private String tenantId;

    @Column(nullable = false)
    private boolean enabled = true;

    @Column(name = "email_verified")
    private boolean emailVerified = false;

    @Column(name = "last_login_at")
    private Instant lastLoginAt;

    @Column(name = "sso_provider")
    private String ssoProvider;

    @Column(name = "sso_subject_id")
    private String ssoSubjectId;
}

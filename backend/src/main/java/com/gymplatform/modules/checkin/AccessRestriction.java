package com.gymplatform.modules.checkin;

import com.gymplatform.shared.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "access_restrictions")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AccessRestriction extends BaseEntity {

    @Column(name = "member_id", nullable = false)
    private UUID memberId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private RestrictionReason reason;

    @Column(columnDefinition = "text")
    private String description;

    @Column(nullable = false)
    private boolean active = true;

    @Column(name = "restricted_by")
    private UUID restrictedBy;

    @Column(name = "restricted_at")
    private Instant restrictedAt;

    @Column(name = "released_at")
    private Instant releasedAt;

    @Column(name = "released_by")
    private UUID releasedBy;

    @Column(name = "auto_release")
    private boolean autoRelease;

    @Column(name = "tenant_id", length = 100)
    private String tenantId;
}

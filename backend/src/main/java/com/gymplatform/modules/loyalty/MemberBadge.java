package com.gymplatform.modules.loyalty;

import com.gymplatform.shared.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "member_badges")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class MemberBadge extends BaseEntity {

    @Column(name = "member_id", nullable = false)
    private UUID memberId;

    @Column(name = "badge_id", nullable = false)
    private UUID badgeId;

    @Column(name = "earned_at", nullable = false)
    private Instant earnedAt;

    @Column(name = "tenant_id", length = 100)
    private String tenantId;
}

package com.gymplatform.modules.loyalty;

import com.gymplatform.shared.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "referrals")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Referral extends BaseEntity {

    @Column(name = "referrer_member_id", nullable = false)
    private UUID referrerMemberId;

    @Column(name = "referred_member_id")
    private UUID referredMemberId;

    @Column(name = "referred_email", length = 255)
    private String referredEmail;

    @Column(name = "referral_code", nullable = false, length = 50)
    private String referralCode;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ReferralStatus status;

    @Column(name = "referrer_points_awarded")
    private int referrerPointsAwarded;

    @Column(name = "referred_points_awarded")
    private int referredPointsAwarded;

    @Column(name = "converted_at")
    private Instant convertedAt;

    @Column(name = "tenant_id", length = 100)
    private String tenantId;
}

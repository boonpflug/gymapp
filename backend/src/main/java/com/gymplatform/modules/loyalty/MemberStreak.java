package com.gymplatform.modules.loyalty;

import com.gymplatform.shared.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "member_streaks")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class MemberStreak extends BaseEntity {

    @Column(name = "member_id", nullable = false)
    private UUID memberId;

    @Enumerated(EnumType.STRING)
    @Column(name = "streak_type", nullable = false, length = 20)
    private StreakType streakType;

    @Column(name = "current_streak", nullable = false)
    private int currentStreak;

    @Column(name = "longest_streak", nullable = false)
    private int longestStreak;

    @Column(name = "last_activity_date")
    private LocalDate lastActivityDate;

    @Column(name = "streak_start_date")
    private LocalDate streakStartDate;

    @Column(name = "tenant_id", length = 100)
    private String tenantId;
}

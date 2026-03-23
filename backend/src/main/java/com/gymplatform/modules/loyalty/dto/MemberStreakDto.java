package com.gymplatform.modules.loyalty.dto;

import com.gymplatform.modules.loyalty.StreakType;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDate;
import java.util.UUID;

@Data
@Builder
public class MemberStreakDto {
    private UUID id;
    private UUID memberId;
    private StreakType streakType;
    private int currentStreak;
    private int longestStreak;
    private LocalDate lastActivityDate;
    private LocalDate streakStartDate;
}

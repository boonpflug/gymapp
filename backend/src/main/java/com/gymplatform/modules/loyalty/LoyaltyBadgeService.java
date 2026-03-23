package com.gymplatform.modules.loyalty;

import com.gymplatform.config.multitenancy.TenantContext;
import com.gymplatform.modules.checkin.CheckInRepository;
import com.gymplatform.modules.loyalty.dto.*;
import com.gymplatform.modules.member.Member;
import com.gymplatform.modules.member.MemberRepository;
import com.gymplatform.modules.training.TrainingSessionRepository;
import com.gymplatform.shared.AuditLogService;
import com.gymplatform.shared.BusinessException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.time.Period;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class LoyaltyBadgeService {

    private final LoyaltyBadgeRepository badgeRepository;
    private final MemberBadgeRepository memberBadgeRepository;
    private final MemberStreakRepository memberStreakRepository;
    private final ReferralRepository referralRepository;
    private final MemberRepository memberRepository;
    private final CheckInRepository checkInRepository;
    private final TrainingSessionRepository trainingSessionRepository;
    private final AuditLogService auditLogService;

    @Transactional
    public LoyaltyBadgeDto createBadge(CreateBadgeRequest req, UUID userId) {
        LoyaltyBadge badge = LoyaltyBadge.builder()
                .name(req.getName())
                .description(req.getDescription())
                .icon(req.getIcon())
                .category(req.getCategory())
                .criteriaType(req.getCriteriaType())
                .criteriaValue(req.getCriteriaValue())
                .active(true)
                .tenantId(TenantContext.getTenantId())
                .build();
        badge = badgeRepository.save(badge);

        auditLogService.log("LoyaltyBadge", badge.getId(), "CREATE", userId, null, null);
        log.info("Created loyalty badge '{}' (id={})", badge.getName(), badge.getId());

        return toDto(badge);
    }

    @Transactional(readOnly = true)
    public List<LoyaltyBadgeDto> listBadges() {
        return badgeRepository.findByActiveTrueOrderByNameAsc()
                .stream()
                .map(this::toDto)
                .toList();
    }

    @Transactional
    public LoyaltyBadgeDto updateBadge(UUID id, CreateBadgeRequest req, UUID userId) {
        LoyaltyBadge badge = badgeRepository.findById(id)
                .orElseThrow(() -> BusinessException.notFound("LoyaltyBadge", id));

        badge.setName(req.getName());
        badge.setDescription(req.getDescription());
        badge.setIcon(req.getIcon());
        badge.setCategory(req.getCategory());
        badge.setCriteriaType(req.getCriteriaType());
        badge.setCriteriaValue(req.getCriteriaValue());
        badge = badgeRepository.save(badge);

        auditLogService.log("LoyaltyBadge", badge.getId(), "UPDATE", userId, null, null);
        log.info("Updated loyalty badge '{}' (id={})", badge.getName(), badge.getId());

        return toDto(badge);
    }

    @Transactional
    public void deactivateBadge(UUID id, UUID userId) {
        LoyaltyBadge badge = badgeRepository.findById(id)
                .orElseThrow(() -> BusinessException.notFound("LoyaltyBadge", id));

        badge.setActive(false);
        badgeRepository.save(badge);

        auditLogService.log("LoyaltyBadge", badge.getId(), "DEACTIVATE", userId, null, null);
        log.info("Deactivated loyalty badge '{}' (id={})", badge.getName(), badge.getId());
    }

    @Transactional(readOnly = true)
    public List<MemberBadgeDto> getMemberBadges(UUID memberId) {
        return memberBadgeRepository.findByMemberIdOrderByEarnedAtDesc(memberId)
                .stream()
                .map(this::toMemberBadgeDto)
                .toList();
    }

    @Transactional
    public List<MemberBadgeDto> checkAndAwardBadges(UUID memberId) {
        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> BusinessException.notFound("Member", memberId));

        List<LoyaltyBadge> activeBadges = badgeRepository.findByActiveTrueOrderByNameAsc();
        List<MemberBadgeDto> newlyAwarded = new ArrayList<>();

        for (LoyaltyBadge badge : activeBadges) {
            if (memberBadgeRepository.existsByMemberIdAndBadgeId(memberId, badge.getId())) {
                continue;
            }

            Integer criteriaValue = badge.getCriteriaValue();
            if (criteriaValue == null) {
                continue;
            }

            boolean eligible = false;

            switch (badge.getCriteriaType()) {
                case CHECKIN_COUNT -> {
                    long checkins = checkInRepository
                            .findByMemberIdOrderByCheckInTimeDesc(memberId, Pageable.unpaged())
                            .getTotalElements();
                    eligible = checkins >= criteriaValue;
                }
                case SESSION_COUNT -> {
                    long sessions = trainingSessionRepository
                            .countSessionsSince(memberId, Instant.EPOCH);
                    eligible = sessions >= criteriaValue;
                }
                case MEMBER_DURATION_MONTHS -> {
                    LocalDate joinDate = member.getJoinDate();
                    if (joinDate != null) {
                        int monthsSinceJoin = Period.between(joinDate, LocalDate.now()).getYears() * 12
                                + Period.between(joinDate, LocalDate.now()).getMonths();
                        eligible = monthsSinceJoin >= criteriaValue;
                    }
                }
                case REFERRAL_COUNT -> {
                    long convertedReferrals = referralRepository
                            .countByReferrerMemberIdAndStatus(memberId, ReferralStatus.CONVERTED);
                    eligible = convertedReferrals >= criteriaValue;
                }
                case STREAK_DAYS -> {
                    List<MemberStreak> streaks = memberStreakRepository.findByMemberId(memberId);
                    eligible = streaks.stream()
                            .anyMatch(s -> s.getCurrentStreak() >= criteriaValue);
                }
                case CUSTOM -> {
                    // Custom badges are awarded manually
                }
            }

            if (eligible) {
                MemberBadge memberBadge = MemberBadge.builder()
                        .memberId(memberId)
                        .badgeId(badge.getId())
                        .earnedAt(Instant.now())
                        .tenantId(TenantContext.getTenantId())
                        .build();
                memberBadge = memberBadgeRepository.save(memberBadge);

                auditLogService.log("MemberBadge", memberBadge.getId(), "AWARD", null, null,
                        "Badge '" + badge.getName() + "' awarded to member " + memberId);
                log.info("Awarded badge '{}' to member {}", badge.getName(), memberId);

                newlyAwarded.add(toMemberBadgeDto(memberBadge));
            }
        }

        return newlyAwarded;
    }

    @Transactional(readOnly = true)
    public List<MemberStreakDto> getMemberStreaks(UUID memberId) {
        return memberStreakRepository.findByMemberId(memberId).stream()
                .map(this::toStreakDto)
                .collect(java.util.stream.Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<MemberStreakDto> getStreakLeaderboard(StreakType streakType) {
        return memberStreakRepository.findTop10ByStreakTypeOrderByCurrentStreakDesc(streakType).stream()
                .map(this::toStreakDto)
                .collect(java.util.stream.Collectors.toList());
    }

    @Transactional
    public MemberStreakDto updateStreak(UUID memberId, StreakType streakType) {
        MemberStreak streak = memberStreakRepository.findByMemberIdAndStreakType(memberId, streakType)
                .orElse(null);

        LocalDate today = LocalDate.now();

        if (streak == null) {
            streak = MemberStreak.builder()
                    .memberId(memberId)
                    .streakType(streakType)
                    .currentStreak(1)
                    .longestStreak(1)
                    .lastActivityDate(today)
                    .streakStartDate(today)
                    .tenantId(TenantContext.getTenantId())
                    .build();
        } else {
            LocalDate lastActivity = streak.getLastActivityDate();

            if (lastActivity != null && lastActivity.equals(today)) {
                // Already recorded activity today, no change
                return toStreakDto(streak);
            }

            boolean isContinuation;
            if (streakType == StreakType.DAILY_CHECKIN) {
                isContinuation = lastActivity != null && lastActivity.equals(today.minusDays(1));
            } else {
                // WEEKLY_CHECKIN: activity within the last 7 days continues the streak
                isContinuation = lastActivity != null && !lastActivity.isBefore(today.minusDays(7));
            }

            if (isContinuation) {
                streak.setCurrentStreak(streak.getCurrentStreak() + 1);
            } else {
                streak.setCurrentStreak(1);
                streak.setStreakStartDate(today);
            }

            if (streak.getCurrentStreak() > streak.getLongestStreak()) {
                streak.setLongestStreak(streak.getCurrentStreak());
            }

            streak.setLastActivityDate(today);
        }

        streak = memberStreakRepository.save(streak);
        log.debug("Updated streak for member {}: type={}, current={}, longest={}",
                memberId, streakType, streak.getCurrentStreak(), streak.getLongestStreak());

        return toStreakDto(streak);
    }

    // --- mapping helpers ---

    private LoyaltyBadgeDto toDto(LoyaltyBadge badge) {
        return LoyaltyBadgeDto.builder()
                .id(badge.getId())
                .name(badge.getName())
                .description(badge.getDescription())
                .icon(badge.getIcon())
                .category(badge.getCategory())
                .criteriaType(badge.getCriteriaType())
                .criteriaValue(badge.getCriteriaValue())
                .active(badge.isActive())
                .createdAt(badge.getCreatedAt())
                .build();
    }

    private MemberBadgeDto toMemberBadgeDto(MemberBadge mb) {
        LoyaltyBadge badge = badgeRepository.findById(mb.getBadgeId()).orElse(null);
        return MemberBadgeDto.builder()
                .id(mb.getId())
                .memberId(mb.getMemberId())
                .badgeId(mb.getBadgeId())
                .badgeName(badge != null ? badge.getName() : null)
                .badgeDescription(badge != null ? badge.getDescription() : null)
                .badgeIcon(badge != null ? badge.getIcon() : null)
                .badgeCategory(badge != null ? badge.getCategory() : null)
                .earnedAt(mb.getEarnedAt())
                .build();
    }

    private MemberStreakDto toStreakDto(MemberStreak streak) {
        return MemberStreakDto.builder()
                .id(streak.getId())
                .memberId(streak.getMemberId())
                .streakType(streak.getStreakType())
                .currentStreak(streak.getCurrentStreak())
                .longestStreak(streak.getLongestStreak())
                .lastActivityDate(streak.getLastActivityDate())
                .streakStartDate(streak.getStreakStartDate())
                .build();
    }
}

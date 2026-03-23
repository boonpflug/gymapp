package com.gymplatform.modules.loyalty;

import com.gymplatform.config.multitenancy.TenantContext;
import com.gymplatform.modules.loyalty.dto.*;
import com.gymplatform.modules.member.Member;
import com.gymplatform.modules.member.MemberRepository;
import com.gymplatform.shared.AuditLogService;
import com.gymplatform.shared.BusinessException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.YearMonth;
import java.time.ZoneOffset;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class LoyaltyPointsService {

    private final LoyaltyTransactionRepository transactionRepository;
    private final LoyaltyConfigRepository configRepository;
    private final LoyaltyTierRepository tierRepository;
    private final MemberBadgeRepository memberBadgeRepository;
    private final MemberStreakRepository memberStreakRepository;
    private final LoyaltyBadgeRepository badgeRepository;
    private final LoyaltyRedemptionRepository redemptionRepository;
    private final MemberRepository memberRepository;
    private final AuditLogService auditLogService;
    private final RabbitTemplate rabbitTemplate;

    private static final Map<LoyaltyAction, Integer> DEFAULT_POINTS = Map.of(
            LoyaltyAction.CHECK_IN, 10,
            LoyaltyAction.CLASS_BOOKING, 15,
            LoyaltyAction.SESSION_COMPLETE, 20,
            LoyaltyAction.REFERRAL, 50,
            LoyaltyAction.BIRTHDAY, 100,
            LoyaltyAction.ANNIVERSARY, 200,
            LoyaltyAction.GOAL_ACHIEVED, 30
    );

    @Transactional(readOnly = true)
    public Map<LoyaltyAction, Integer> getPointsConfig() {
        Map<LoyaltyAction, Integer> config = new EnumMap<>(LoyaltyAction.class);

        // Start with defaults
        config.putAll(DEFAULT_POINTS);

        // Override with database values
        List<LoyaltyConfig> dbConfigs = configRepository.findAllByOrderByConfigKeyAsc();
        for (LoyaltyConfig c : dbConfigs) {
            String key = c.getConfigKey();
            if (key != null && key.startsWith("points.")) {
                String actionName = key.substring("points.".length());
                try {
                    LoyaltyAction action = LoyaltyAction.valueOf(actionName);
                    config.put(action, Integer.parseInt(c.getConfigValue()));
                } catch (IllegalArgumentException e) {
                    log.warn("Unknown loyalty action in config: {}", actionName);
                }
            }
        }

        return config;
    }

    @Transactional
    public void setPointsConfig(LoyaltyAction action, int points) {
        String configKey = "points." + action.name();
        LoyaltyConfig config = configRepository.findByConfigKey(configKey)
                .orElse(LoyaltyConfig.builder()
                        .configKey(configKey)
                        .description("Points awarded for " + action.name())
                        .tenantId(TenantContext.getTenantId())
                        .build());

        String oldValue = config.getConfigValue();
        config.setConfigValue(String.valueOf(points));
        configRepository.save(config);

        auditLogService.log("LoyaltyConfig", config.getId(), "UPDATE_POINTS_CONFIG",
                null, oldValue, String.valueOf(points));

        log.info("Updated points config for action {} to {} points", action, points);
    }

    @Transactional
    public LoyaltyTransactionDto awardPoints(UUID memberId, int points, LoyaltyAction action,
                                              UUID referenceId, String description) {
        if (points <= 0) {
            throw BusinessException.badRequest("Points to award must be positive");
        }

        int currentBalance = transactionRepository.sumPointsByMemberId(memberId);
        int balanceAfter = currentBalance + points;

        LoyaltyTransaction transaction = LoyaltyTransaction.builder()
                .memberId(memberId)
                .points(points)
                .balanceAfter(balanceAfter)
                .transactionType(TransactionType.EARN)
                .action(action)
                .referenceId(referenceId)
                .description(description)
                .tenantId(TenantContext.getTenantId())
                .build();

        transaction = transactionRepository.save(transaction);

        log.info("Awarded {} points to member {} for action {}", points, memberId, action);

        // Publish RabbitMQ event
        try {
            rabbitTemplate.convertAndSend("notification.events", "loyalty.points.earned", Map.of(
                    "memberId", memberId.toString(),
                    "points", points,
                    "action", action.name(),
                    "balanceAfter", balanceAfter,
                    "transactionId", transaction.getId().toString()
            ));
        } catch (Exception e) {
            log.error("Failed to publish loyalty.points.earned event for member {}", memberId, e);
        }

        // Check badge eligibility
        checkBadgeEligibility(memberId);

        auditLogService.log("LoyaltyTransaction", transaction.getId(), "AWARD_POINTS",
                null, null, points + " points for " + action.name());

        return toTransactionDto(transaction);
    }

    @Transactional
    public LoyaltyTransactionDto awardPointsForAction(UUID memberId, LoyaltyAction action, UUID referenceId) {
        Map<LoyaltyAction, Integer> config = getPointsConfig();
        int points = config.getOrDefault(action, DEFAULT_POINTS.getOrDefault(action, 0));

        if (points == 0) {
            throw BusinessException.badRequest("No points configured for action: " + action);
        }

        String description = "Points earned for " + action.name().toLowerCase().replace('_', ' ');
        return awardPoints(memberId, points, action, referenceId, description);
    }

    @Transactional
    public LoyaltyTransactionDto deductPoints(UUID memberId, int points, String description) {
        if (points <= 0) {
            throw BusinessException.badRequest("Points to deduct must be positive");
        }

        int currentBalance = transactionRepository.sumPointsByMemberId(memberId);
        if (currentBalance < points) {
            throw BusinessException.badRequest(
                    "Insufficient points. Current balance: " + currentBalance + ", requested: " + points);
        }

        int balanceAfter = currentBalance - points;

        LoyaltyTransaction transaction = LoyaltyTransaction.builder()
                .memberId(memberId)
                .points(-points)
                .balanceAfter(balanceAfter)
                .transactionType(TransactionType.REDEEM)
                .action(LoyaltyAction.REDEMPTION)
                .description(description)
                .tenantId(TenantContext.getTenantId())
                .build();

        transaction = transactionRepository.save(transaction);

        log.info("Deducted {} points from member {}", points, memberId);

        auditLogService.log("LoyaltyTransaction", transaction.getId(), "DEDUCT_POINTS",
                null, null, "-" + points + " points");

        return toTransactionDto(transaction);
    }

    @Transactional(readOnly = true)
    public int getBalance(UUID memberId) {
        return transactionRepository.sumPointsByMemberId(memberId);
    }

    @Transactional(readOnly = true)
    public Page<LoyaltyTransactionDto> getTransactionHistory(UUID memberId, Pageable pageable) {
        return transactionRepository.findByMemberIdOrderByCreatedAtDesc(memberId, pageable)
                .map(this::toTransactionDto);
    }

    @Transactional(readOnly = true)
    public MemberLoyaltySummaryDto getMemberSummary(UUID memberId) {
        int currentPoints = transactionRepository.sumPointsByMemberId(memberId);
        int totalEarned = transactionRepository.sumEarnedPointsByMemberId(memberId);

        LoyaltyTierDto currentTierDto = null;
        LoyaltyTierDto nextTierDto = null;
        int pointsToNextTier = 0;

        LoyaltyTier currentTier = tierRepository
                .findFirstByMinPointsLessThanEqualAndActiveTrueOrderByMinPointsDesc(totalEarned)
                .orElse(null);

        if (currentTier != null) {
            currentTierDto = toTierDto(currentTier);

            // Find next tier
            List<LoyaltyTier> allTiers = tierRepository.findByActiveTrueOrderBySortOrderAsc();
            LoyaltyTier nextTier = null;
            for (LoyaltyTier tier : allTiers) {
                if (tier.getMinPoints() > totalEarned) {
                    nextTier = tier;
                    break;
                }
            }
            if (nextTier != null) {
                nextTierDto = toTierDto(nextTier);
                pointsToNextTier = nextTier.getMinPoints() - totalEarned;
            }
        }

        List<MemberStreakDto> streaks = memberStreakRepository.findByMemberId(memberId).stream()
                .map(this::toStreakDto)
                .collect(Collectors.toList());

        List<MemberBadgeDto> badges = memberBadgeRepository.findByMemberIdOrderByEarnedAtDesc(memberId).stream()
                .map(this::toBadgeDto)
                .collect(Collectors.toList());

        long totalRedemptions = redemptionRepository.findByMemberIdOrderByCreatedAtDesc(memberId, Pageable.unpaged())
                .getTotalElements();

        return MemberLoyaltySummaryDto.builder()
                .memberId(memberId)
                .currentPoints(currentPoints)
                .totalEarned(totalEarned)
                .currentTier(currentTierDto)
                .nextTier(nextTierDto)
                .pointsToNextTier(pointsToNextTier)
                .streaks(streaks)
                .badges(badges)
                .totalBadges(badges.size())
                .totalRedemptions((int) totalRedemptions)
                .build();
    }

    @Transactional(readOnly = true)
    public LoyaltyTierDto getCurrentTier(UUID memberId) {
        int totalEarned = transactionRepository.sumEarnedPointsByMemberId(memberId);
        return tierRepository
                .findFirstByMinPointsLessThanEqualAndActiveTrueOrderByMinPointsDesc(totalEarned)
                .map(this::toTierDto)
                .orElse(null);
    }

    @Transactional(readOnly = true)
    public LoyaltyDashboardDto getDashboard() {
        YearMonth currentMonth = YearMonth.now();
        Instant startOfMonth = currentMonth.atDay(1).atStartOfDay().toInstant(ZoneOffset.UTC);

        int pointsIssued = transactionRepository.sumPointsIssuedSince(startOfMonth);
        int pointsRedeemed = redemptionRepository.sumPointsRedeemedSince(startOfMonth);
        long redemptionCount = redemptionRepository.countRedemptionsSince(startOfMonth);
        long totalParticipants = transactionRepository.countDistinctMembers();

        // Top 10 members by total earned points
        List<TopMemberDto> topMembers = buildTopMembers();

        return LoyaltyDashboardDto.builder()
                .pointsIssuedThisMonth(pointsIssued)
                .pointsRedeemedThisMonth(pointsRedeemed)
                .redemptionsThisMonth(redemptionCount)
                .totalParticipants(totalParticipants)
                .topMembers(topMembers)
                .build();
    }

    // ─── Tier management ──────────────────────────────────────────────

    @Transactional(readOnly = true)
    public List<LoyaltyTierDto> listActiveTiers() {
        return tierRepository.findByActiveTrueOrderBySortOrderAsc().stream()
                .map(this::toTierDto)
                .collect(Collectors.toList());
    }

    @Transactional
    public LoyaltyTierDto createTier(CreateLoyaltyTierRequest req, UUID userId) {
        LoyaltyTier tier = LoyaltyTier.builder()
                .name(req.getName())
                .minPoints(req.getMinPoints())
                .color(req.getColor())
                .icon(req.getIcon())
                .perks(req.getPerks())
                .sortOrder(req.getSortOrder())
                .active(true)
                .tenantId(TenantContext.getTenantId())
                .build();
        tier = tierRepository.save(tier);
        auditLogService.log("LoyaltyTier", tier.getId(), "CREATE", userId, null, null);
        log.info("Created loyalty tier: {}", tier.getName());
        return toTierDto(tier);
    }

    @Transactional
    public LoyaltyTierDto updateTier(UUID id, CreateLoyaltyTierRequest req, UUID userId) {
        LoyaltyTier tier = tierRepository.findById(id)
                .orElseThrow(() -> BusinessException.notFound("LoyaltyTier", id));
        tier.setName(req.getName());
        tier.setMinPoints(req.getMinPoints());
        tier.setColor(req.getColor());
        tier.setIcon(req.getIcon());
        tier.setPerks(req.getPerks());
        tier.setSortOrder(req.getSortOrder());
        tier = tierRepository.save(tier);
        auditLogService.log("LoyaltyTier", tier.getId(), "UPDATE", userId, null, null);
        return toTierDto(tier);
    }

    // ─── Private helpers ──────────────────────────────────────────────

    private List<TopMemberDto> buildTopMembers() {
        // Get all transactions grouped by member, sorted by total earned desc
        List<LoyaltyTransaction> allTransactions = transactionRepository.findAll();
        Map<UUID, Integer> memberTotals = new HashMap<>();
        for (LoyaltyTransaction t : allTransactions) {
            if (t.getTransactionType() == TransactionType.EARN) {
                memberTotals.merge(t.getMemberId(), t.getPoints(), Integer::sum);
            }
        }

        return memberTotals.entrySet().stream()
                .sorted(Map.Entry.<UUID, Integer>comparingByValue().reversed())
                .limit(10)
                .map(entry -> {
                    UUID memberId = entry.getKey();
                    int totalPoints = entry.getValue();
                    String memberName = getMemberName(memberId);
                    String tierName = tierRepository
                            .findFirstByMinPointsLessThanEqualAndActiveTrueOrderByMinPointsDesc(totalPoints)
                            .map(LoyaltyTier::getName)
                            .orElse(null);

                    return TopMemberDto.builder()
                            .memberId(memberId)
                            .memberName(memberName)
                            .totalPoints(totalPoints)
                            .tierName(tierName)
                            .build();
                })
                .collect(Collectors.toList());
    }

    private void checkBadgeEligibility(UUID memberId) {
        List<LoyaltyBadge> activeBadges = badgeRepository.findByActiveTrueOrderByNameAsc();

        for (LoyaltyBadge badge : activeBadges) {
            // Skip if member already has this badge
            if (memberBadgeRepository.existsByMemberIdAndBadgeId(memberId, badge.getId())) {
                continue;
            }

            boolean eligible = false;
            Integer criteriaValue = badge.getCriteriaValue();
            if (criteriaValue == null) {
                continue;
            }

            switch (badge.getCriteriaType()) {
                case CHECKIN_COUNT -> {
                    long checkins = transactionRepository
                            .findByMemberIdAndActionOrderByCreatedAtDesc(memberId, LoyaltyAction.CHECK_IN)
                            .size();
                    eligible = checkins >= criteriaValue;
                }
                case SESSION_COUNT -> {
                    long sessions = transactionRepository
                            .findByMemberIdAndActionOrderByCreatedAtDesc(memberId, LoyaltyAction.SESSION_COMPLETE)
                            .size();
                    eligible = sessions >= criteriaValue;
                }
                case REFERRAL_COUNT -> {
                    long referrals = transactionRepository
                            .findByMemberIdAndActionOrderByCreatedAtDesc(memberId, LoyaltyAction.REFERRAL)
                            .size();
                    eligible = referrals >= criteriaValue;
                }
                case STREAK_DAYS -> {
                    List<MemberStreak> streaks = memberStreakRepository.findByMemberId(memberId);
                    eligible = streaks.stream()
                            .anyMatch(s -> s.getCurrentStreak() >= criteriaValue);
                }
                case MEMBER_DURATION_MONTHS, CUSTOM -> {
                    // These require external data or manual awarding
                }
            }

            if (eligible) {
                MemberBadge memberBadge = MemberBadge.builder()
                        .memberId(memberId)
                        .badgeId(badge.getId())
                        .earnedAt(Instant.now())
                        .tenantId(TenantContext.getTenantId())
                        .build();
                memberBadgeRepository.save(memberBadge);

                log.info("Awarded badge '{}' to member {}", badge.getName(), memberId);

                try {
                    rabbitTemplate.convertAndSend("notification.events", "loyalty.badge.earned", Map.of(
                            "memberId", memberId.toString(),
                            "badgeId", badge.getId().toString(),
                            "badgeName", badge.getName()
                    ));
                } catch (Exception e) {
                    log.error("Failed to publish loyalty.badge.earned event for member {}", memberId, e);
                }
            }
        }
    }

    private String getMemberName(UUID memberId) {
        return memberRepository.findById(memberId)
                .map(m -> m.getFirstName() + " " + m.getLastName())
                .orElse("Unknown");
    }

    private LoyaltyTransactionDto toTransactionDto(LoyaltyTransaction t) {
        return LoyaltyTransactionDto.builder()
                .id(t.getId())
                .memberId(t.getMemberId())
                .memberName(getMemberName(t.getMemberId()))
                .points(t.getPoints())
                .balanceAfter(t.getBalanceAfter())
                .transactionType(t.getTransactionType())
                .action(t.getAction())
                .referenceId(t.getReferenceId())
                .description(t.getDescription())
                .createdAt(t.getCreatedAt())
                .build();
    }

    private LoyaltyTierDto toTierDto(LoyaltyTier tier) {
        return LoyaltyTierDto.builder()
                .id(tier.getId())
                .name(tier.getName())
                .minPoints(tier.getMinPoints())
                .color(tier.getColor())
                .icon(tier.getIcon())
                .perks(tier.getPerks())
                .sortOrder(tier.getSortOrder())
                .active(tier.isActive())
                .createdAt(tier.getCreatedAt())
                .build();
    }

    private MemberBadgeDto toBadgeDto(MemberBadge mb) {
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

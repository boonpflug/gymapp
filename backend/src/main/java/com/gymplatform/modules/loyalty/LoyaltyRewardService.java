package com.gymplatform.modules.loyalty;

import com.gymplatform.config.multitenancy.TenantContext;
import com.gymplatform.modules.loyalty.dto.*;
import com.gymplatform.modules.member.Member;
import com.gymplatform.modules.member.MemberRepository;
import com.gymplatform.shared.AuditLogService;
import com.gymplatform.shared.BusinessException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class LoyaltyRewardService {

    private final LoyaltyRewardRepository rewardRepository;
    private final LoyaltyRedemptionRepository redemptionRepository;
    private final LoyaltyPointsService loyaltyPointsService;
    private final MemberRepository memberRepository;
    private final AuditLogService auditLogService;

    @Transactional
    public LoyaltyRewardDto createReward(CreateRewardRequest req, UUID userId) {
        LoyaltyReward reward = LoyaltyReward.builder()
                .name(req.getName())
                .description(req.getDescription())
                .rewardType(req.getRewardType())
                .pointsCost(req.getPointsCost())
                .value(req.getValue())
                .active(true)
                .imageUrl(req.getImageUrl())
                .maxRedemptionsPerMember(req.getMaxRedemptionsPerMember())
                .totalAvailable(req.getTotalAvailable())
                .totalRedeemed(0)
                .tenantId(TenantContext.getTenantId())
                .build();
        reward = rewardRepository.save(reward);

        auditLogService.log("LoyaltyReward", reward.getId(), "CREATE", userId, null, null);
        log.info("Created loyalty reward '{}' (id={})", reward.getName(), reward.getId());

        return toDto(reward);
    }

    @Transactional(readOnly = true)
    public Page<LoyaltyRewardDto> listRewards(Pageable pageable) {
        return rewardRepository.findAllByOrderByCreatedAtDesc(pageable).map(this::toDto);
    }

    @Transactional(readOnly = true)
    public List<LoyaltyRewardDto> listActiveRewards() {
        return rewardRepository.findByActiveTrueOrderByPointsCostAsc()
                .stream()
                .map(this::toDto)
                .toList();
    }

    @Transactional
    public LoyaltyRewardDto updateReward(UUID id, CreateRewardRequest req, UUID userId) {
        LoyaltyReward reward = rewardRepository.findById(id)
                .orElseThrow(() -> BusinessException.notFound("LoyaltyReward", id));

        reward.setName(req.getName());
        reward.setDescription(req.getDescription());
        reward.setRewardType(req.getRewardType());
        reward.setPointsCost(req.getPointsCost());
        reward.setValue(req.getValue());
        reward.setImageUrl(req.getImageUrl());
        reward.setMaxRedemptionsPerMember(req.getMaxRedemptionsPerMember());
        reward.setTotalAvailable(req.getTotalAvailable());
        reward = rewardRepository.save(reward);

        auditLogService.log("LoyaltyReward", reward.getId(), "UPDATE", userId, null, null);
        log.info("Updated loyalty reward '{}' (id={})", reward.getName(), reward.getId());

        return toDto(reward);
    }

    @Transactional
    public void deactivateReward(UUID id, UUID userId) {
        LoyaltyReward reward = rewardRepository.findById(id)
                .orElseThrow(() -> BusinessException.notFound("LoyaltyReward", id));

        reward.setActive(false);
        rewardRepository.save(reward);

        auditLogService.log("LoyaltyReward", reward.getId(), "DEACTIVATE", userId, null, null);
        log.info("Deactivated loyalty reward '{}' (id={})", reward.getName(), reward.getId());
    }

    @Transactional
    public LoyaltyRedemptionDto redeemReward(UUID memberId, UUID rewardId, UUID userId) {
        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> BusinessException.notFound("Member", memberId));

        LoyaltyReward reward = rewardRepository.findById(rewardId)
                .orElseThrow(() -> BusinessException.notFound("LoyaltyReward", rewardId));

        if (!reward.isActive()) {
            throw BusinessException.badRequest("Reward is not active");
        }

        int balance = loyaltyPointsService.getBalance(memberId);
        if (balance < reward.getPointsCost()) {
            throw BusinessException.badRequest(
                    "Insufficient points. Required: " + reward.getPointsCost() + ", available: " + balance);
        }

        if (reward.getMaxRedemptionsPerMember() != null) {
            long memberRedemptions = redemptionRepository.countByMemberIdAndRewardId(memberId, rewardId);
            if (memberRedemptions >= reward.getMaxRedemptionsPerMember()) {
                throw BusinessException.badRequest(
                        "Maximum redemptions per member reached (" + reward.getMaxRedemptionsPerMember() + ")");
            }
        }

        if (reward.getTotalAvailable() != null && reward.getTotalRedeemed() >= reward.getTotalAvailable()) {
            throw BusinessException.badRequest("Reward is no longer available (all units redeemed)");
        }

        LoyaltyTransactionDto transactionDto = loyaltyPointsService.deductPoints(
                memberId, reward.getPointsCost(), "Redeemed reward: " + reward.getName());
        UUID transactionId = transactionDto.getId();

        LoyaltyRedemption redemption = LoyaltyRedemption.builder()
                .memberId(memberId)
                .rewardId(rewardId)
                .transactionId(transactionId)
                .pointsSpent(reward.getPointsCost())
                .status(RedemptionStatus.PENDING)
                .tenantId(TenantContext.getTenantId())
                .build();
        redemption = redemptionRepository.save(redemption);

        reward.setTotalRedeemed(reward.getTotalRedeemed() + 1);
        rewardRepository.save(reward);

        auditLogService.log("LoyaltyRedemption", redemption.getId(), "REDEEM", userId, null,
                "rewardId=" + rewardId + ", points=" + reward.getPointsCost());
        log.info("Member {} redeemed reward '{}' for {} points", memberId, reward.getName(), reward.getPointsCost());

        return toRedemptionDto(redemption, member, reward);
    }

    @Transactional
    public LoyaltyRedemptionDto fulfillRedemption(UUID redemptionId, UUID userId) {
        LoyaltyRedemption redemption = redemptionRepository.findById(redemptionId)
                .orElseThrow(() -> BusinessException.notFound("LoyaltyRedemption", redemptionId));

        if (redemption.getStatus() != RedemptionStatus.PENDING) {
            throw BusinessException.badRequest("Redemption is not in PENDING status");
        }

        redemption.setStatus(RedemptionStatus.FULFILLED);
        redemption.setFulfilledAt(Instant.now());
        redemption = redemptionRepository.save(redemption);

        auditLogService.log("LoyaltyRedemption", redemption.getId(), "FULFILL", userId, null, null);
        log.info("Fulfilled redemption {}", redemptionId);

        return toRedemptionDto(redemption);
    }

    @Transactional
    public LoyaltyRedemptionDto cancelRedemption(UUID redemptionId, UUID userId) {
        LoyaltyRedemption redemption = redemptionRepository.findById(redemptionId)
                .orElseThrow(() -> BusinessException.notFound("LoyaltyRedemption", redemptionId));

        if (redemption.getStatus() != RedemptionStatus.PENDING) {
            throw BusinessException.badRequest("Redemption is not in PENDING status");
        }

        redemption.setStatus(RedemptionStatus.CANCELLED);
        redemption = redemptionRepository.save(redemption);

        loyaltyPointsService.awardPoints(redemption.getMemberId(), redemption.getPointsSpent(),
                LoyaltyAction.MANUAL_ADJUST, redemption.getRewardId(),
                "Refund for cancelled redemption: " + redemptionId);

        auditLogService.log("LoyaltyRedemption", redemption.getId(), "CANCEL", userId, null,
                "Refunded " + redemption.getPointsSpent() + " points");
        log.info("Cancelled redemption {} and refunded {} points", redemptionId, redemption.getPointsSpent());

        return toRedemptionDto(redemption);
    }

    @Transactional(readOnly = true)
    public Page<LoyaltyRedemptionDto> getMemberRedemptions(UUID memberId, Pageable pageable) {
        return redemptionRepository.findByMemberIdOrderByCreatedAtDesc(memberId, pageable)
                .map(this::toRedemptionDto);
    }

    // --- mapping helpers ---

    private LoyaltyRewardDto toDto(LoyaltyReward reward) {
        return LoyaltyRewardDto.builder()
                .id(reward.getId())
                .name(reward.getName())
                .description(reward.getDescription())
                .rewardType(reward.getRewardType())
                .pointsCost(reward.getPointsCost())
                .value(reward.getValue())
                .active(reward.isActive())
                .imageUrl(reward.getImageUrl())
                .maxRedemptionsPerMember(reward.getMaxRedemptionsPerMember())
                .totalAvailable(reward.getTotalAvailable())
                .totalRedeemed(reward.getTotalRedeemed())
                .createdAt(reward.getCreatedAt())
                .build();
    }

    private LoyaltyRedemptionDto toRedemptionDto(LoyaltyRedemption redemption) {
        String memberName = memberRepository.findById(redemption.getMemberId())
                .map(m -> m.getFirstName() + " " + m.getLastName())
                .orElse(null);
        String rewardName = rewardRepository.findById(redemption.getRewardId())
                .map(LoyaltyReward::getName)
                .orElse(null);
        return buildRedemptionDto(redemption, memberName, rewardName);
    }

    private LoyaltyRedemptionDto toRedemptionDto(LoyaltyRedemption redemption, Member member, LoyaltyReward reward) {
        String memberName = member.getFirstName() + " " + member.getLastName();
        return buildRedemptionDto(redemption, memberName, reward.getName());
    }

    private LoyaltyRedemptionDto buildRedemptionDto(LoyaltyRedemption redemption, String memberName, String rewardName) {
        return LoyaltyRedemptionDto.builder()
                .id(redemption.getId())
                .memberId(redemption.getMemberId())
                .memberName(memberName)
                .rewardId(redemption.getRewardId())
                .rewardName(rewardName)
                .transactionId(redemption.getTransactionId())
                .pointsSpent(redemption.getPointsSpent())
                .status(redemption.getStatus())
                .fulfilledAt(redemption.getFulfilledAt())
                .notes(redemption.getNotes())
                .createdAt(redemption.getCreatedAt())
                .build();
    }
}

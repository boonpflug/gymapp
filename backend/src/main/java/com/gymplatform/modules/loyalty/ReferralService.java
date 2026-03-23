package com.gymplatform.modules.loyalty;

import com.gymplatform.config.multitenancy.TenantContext;
import com.gymplatform.modules.loyalty.dto.LoyaltyTransactionDto;
import com.gymplatform.modules.loyalty.dto.ReferralDto;
import com.gymplatform.modules.member.Member;
import com.gymplatform.modules.member.MemberRepository;
import com.gymplatform.shared.AuditLogService;
import com.gymplatform.shared.BusinessException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class ReferralService {

    private final ReferralRepository referralRepository;
    private final MemberRepository memberRepository;
    private final LoyaltyPointsService loyaltyPointsService;
    private final AuditLogService auditLogService;

    private static final String ALPHANUMERIC = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
    private static final int CODE_LENGTH = 8;
    private static final SecureRandom RANDOM = new SecureRandom();

    @Transactional
    public ReferralDto createReferral(UUID referrerMemberId, String referredEmail) {
        Member referrer = memberRepository.findById(referrerMemberId)
                .orElseThrow(() -> BusinessException.notFound("Member", referrerMemberId));

        referralRepository.findByReferredEmail(referredEmail).ifPresent(existing -> {
            throw BusinessException.conflict("A referral already exists for email: " + referredEmail);
        });

        String code = generateUniqueCode();

        Referral referral = Referral.builder()
                .referrerMemberId(referrerMemberId)
                .referredEmail(referredEmail)
                .referralCode(code)
                .status(ReferralStatus.PENDING)
                .referrerPointsAwarded(0)
                .referredPointsAwarded(0)
                .tenantId(TenantContext.getTenantId())
                .build();

        referral = referralRepository.save(referral);

        log.info("Referral created: code={}, referrer={}, email={}", code, referrerMemberId, referredEmail);
        auditLogService.log("Referral", referral.getId(), "CREATE", referrer.getUserId(), null, null);

        return toDto(referral);
    }

    @Transactional(readOnly = true)
    public ReferralDto getReferralByCode(String code) {
        Referral referral = referralRepository.findByReferralCode(code)
                .orElseThrow(() -> BusinessException.notFound("Referral", code));
        return toDto(referral);
    }

    @Transactional(readOnly = true)
    public List<ReferralDto> getMemberReferrals(UUID memberId) {
        return referralRepository.findByReferrerMemberIdOrderByCreatedAtDesc(memberId)
                .stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    @Transactional
    public ReferralDto convertReferral(String referralCode, UUID newMemberId) {
        Referral referral = referralRepository.findByReferralCode(referralCode)
                .orElseThrow(() -> BusinessException.notFound("Referral", referralCode));

        if (referral.getStatus() != ReferralStatus.PENDING && referral.getStatus() != ReferralStatus.SIGNED_UP) {
            throw BusinessException.badRequest(
                    "Referral cannot be converted. Current status: " + referral.getStatus());
        }

        memberRepository.findById(newMemberId)
                .orElseThrow(() -> BusinessException.notFound("Member", newMemberId));

        referral.setReferredMemberId(newMemberId);
        referral.setStatus(ReferralStatus.CONVERTED);
        referral.setConvertedAt(Instant.now());

        LoyaltyTransactionDto referrerTx = loyaltyPointsService.awardPointsForAction(
                referral.getReferrerMemberId(), LoyaltyAction.REFERRAL, referral.getId());
        LoyaltyTransactionDto referredTx = loyaltyPointsService.awardPointsForAction(
                newMemberId, LoyaltyAction.REFERRAL, referral.getId());

        referral.setReferrerPointsAwarded(referrerTx.getPoints());
        referral.setReferredPointsAwarded(referredTx.getPoints());

        referral = referralRepository.save(referral);

        log.info("Referral converted: code={}, referrer={}, referred={}",
                referralCode, referral.getReferrerMemberId(), newMemberId);
        auditLogService.log("Referral", referral.getId(), "CONVERT",
                null, null, "status=CONVERTED, referredMemberId=" + newMemberId);

        return toDto(referral);
    }

    @Transactional
    public ReferralDto markSignedUp(String referralCode) {
        Referral referral = referralRepository.findByReferralCode(referralCode)
                .orElseThrow(() -> BusinessException.notFound("Referral", referralCode));

        if (referral.getStatus() != ReferralStatus.PENDING) {
            throw BusinessException.badRequest(
                    "Referral cannot be marked as signed up. Current status: " + referral.getStatus());
        }

        referral.setStatus(ReferralStatus.SIGNED_UP);
        referral = referralRepository.save(referral);

        log.info("Referral marked as signed up: code={}", referralCode);
        auditLogService.log("Referral", referral.getId(), "SIGNED_UP", null, null, "status=SIGNED_UP");

        return toDto(referral);
    }

    @Transactional(readOnly = true)
    public Map<String, Object> getReferralStats(UUID memberId) {
        long totalReferrals = referralRepository.findByReferrerMemberIdOrderByCreatedAtDesc(memberId).size();
        long convertedReferrals = referralRepository.countByReferrerMemberIdAndStatus(memberId, ReferralStatus.CONVERTED);
        long pendingReferrals = referralRepository.countByReferrerMemberIdAndStatus(memberId, ReferralStatus.PENDING);

        int totalPointsEarned = referralRepository.findByReferrerMemberIdOrderByCreatedAtDesc(memberId)
                .stream()
                .mapToInt(Referral::getReferrerPointsAwarded)
                .sum();

        Map<String, Object> stats = new LinkedHashMap<>();
        stats.put("totalReferrals", totalReferrals);
        stats.put("convertedReferrals", convertedReferrals);
        stats.put("pendingReferrals", pendingReferrals);
        stats.put("totalPointsEarned", totalPointsEarned);
        return stats;
    }

    private String generateUniqueCode() {
        String code;
        do {
            StringBuilder sb = new StringBuilder(CODE_LENGTH);
            for (int i = 0; i < CODE_LENGTH; i++) {
                sb.append(ALPHANUMERIC.charAt(RANDOM.nextInt(ALPHANUMERIC.length())));
            }
            code = sb.toString();
        } while (referralRepository.existsByReferralCode(code));
        return code;
    }

    private ReferralDto toDto(Referral referral) {
        String referrerName = memberRepository.findById(referral.getReferrerMemberId())
                .map(m -> m.getFirstName() + " " + m.getLastName())
                .orElse(null);

        String referredName = null;
        if (referral.getReferredMemberId() != null) {
            referredName = memberRepository.findById(referral.getReferredMemberId())
                    .map(m -> m.getFirstName() + " " + m.getLastName())
                    .orElse(null);
        }

        return ReferralDto.builder()
                .id(referral.getId())
                .referrerMemberId(referral.getReferrerMemberId())
                .referrerName(referrerName)
                .referredMemberId(referral.getReferredMemberId())
                .referredName(referredName)
                .referredEmail(referral.getReferredEmail())
                .referralCode(referral.getReferralCode())
                .status(referral.getStatus())
                .referrerPointsAwarded(referral.getReferrerPointsAwarded())
                .referredPointsAwarded(referral.getReferredPointsAwarded())
                .convertedAt(referral.getConvertedAt())
                .createdAt(referral.getCreatedAt())
                .build();
    }
}

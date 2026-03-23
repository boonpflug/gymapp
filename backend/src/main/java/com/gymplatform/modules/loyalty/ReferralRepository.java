package com.gymplatform.modules.loyalty;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ReferralRepository extends JpaRepository<Referral, UUID> {
    List<Referral> findByReferrerMemberIdOrderByCreatedAtDesc(UUID referrerMemberId);
    Optional<Referral> findByReferralCode(String referralCode);
    Optional<Referral> findByReferredEmail(String email);
    boolean existsByReferralCode(String code);
    long countByReferrerMemberIdAndStatus(UUID referrerMemberId, ReferralStatus status);
}

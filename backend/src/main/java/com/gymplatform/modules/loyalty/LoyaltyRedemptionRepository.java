package com.gymplatform.modules.loyalty;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Repository
public interface LoyaltyRedemptionRepository extends JpaRepository<LoyaltyRedemption, UUID> {
    Page<LoyaltyRedemption> findByMemberIdOrderByCreatedAtDesc(UUID memberId, Pageable pageable);
    List<LoyaltyRedemption> findByRewardIdAndStatus(UUID rewardId, RedemptionStatus status);
    long countByMemberIdAndRewardId(UUID memberId, UUID rewardId);

    @Query("SELECT COUNT(r) FROM LoyaltyRedemption r WHERE r.createdAt >= :since")
    long countRedemptionsSince(@Param("since") Instant since);

    @Query("SELECT COALESCE(SUM(r.pointsSpent), 0) FROM LoyaltyRedemption r WHERE r.createdAt >= :since")
    int sumPointsRedeemedSince(@Param("since") Instant since);
}

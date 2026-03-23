package com.gymplatform.modules.loyalty;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface LoyaltyTransactionRepository extends JpaRepository<LoyaltyTransaction, UUID> {
    Page<LoyaltyTransaction> findByMemberIdOrderByCreatedAtDesc(UUID memberId, Pageable pageable);
    List<LoyaltyTransaction> findByMemberIdAndActionOrderByCreatedAtDesc(UUID memberId, LoyaltyAction action);
    Optional<LoyaltyTransaction> findFirstByMemberIdOrderByCreatedAtDesc(UUID memberId);

    @Query("SELECT COALESCE(SUM(t.points), 0) FROM LoyaltyTransaction t WHERE t.memberId = :memberId")
    int sumPointsByMemberId(@Param("memberId") UUID memberId);

    @Query("SELECT COALESCE(SUM(t.points), 0) FROM LoyaltyTransaction t WHERE t.memberId = :memberId AND t.transactionType = 'EARN'")
    int sumEarnedPointsByMemberId(@Param("memberId") UUID memberId);

    @Query("SELECT COALESCE(SUM(t.points), 0) FROM LoyaltyTransaction t WHERE t.transactionType = 'EARN' AND t.createdAt >= :since")
    int sumPointsIssuedSince(@Param("since") Instant since);

    @Query("SELECT COUNT(DISTINCT t.memberId) FROM LoyaltyTransaction t WHERE t.transactionType = 'EARN'")
    long countDistinctMembers();
}

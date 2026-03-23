package com.gymplatform.modules.loyalty;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface MemberBadgeRepository extends JpaRepository<MemberBadge, UUID> {
    List<MemberBadge> findByMemberIdOrderByEarnedAtDesc(UUID memberId);
    boolean existsByMemberIdAndBadgeId(UUID memberId, UUID badgeId);
    long countByMemberId(UUID memberId);
}

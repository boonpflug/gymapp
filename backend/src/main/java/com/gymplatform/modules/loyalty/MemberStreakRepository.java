package com.gymplatform.modules.loyalty;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface MemberStreakRepository extends JpaRepository<MemberStreak, UUID> {
    Optional<MemberStreak> findByMemberIdAndStreakType(UUID memberId, StreakType streakType);
    List<MemberStreak> findByMemberId(UUID memberId);
    List<MemberStreak> findTop10ByStreakTypeOrderByCurrentStreakDesc(StreakType streakType);
}

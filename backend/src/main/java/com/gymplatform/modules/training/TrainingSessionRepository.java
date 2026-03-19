package com.gymplatform.modules.training;

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
public interface TrainingSessionRepository extends JpaRepository<TrainingSession, UUID> {

    Page<TrainingSession> findByMemberIdOrderByStartedAtDesc(UUID memberId, Pageable pageable);

    List<TrainingSession> findByMemberIdAndStartedAtBetweenOrderByStartedAtDesc(
            UUID memberId, Instant start, Instant end);

    @Query("SELECT COUNT(s) FROM TrainingSession s WHERE s.memberId = :memberId AND s.startedAt >= :since")
    long countSessionsSince(@Param("memberId") UUID memberId, @Param("since") Instant since);

    List<TrainingSession> findByPlanIdOrderByStartedAtDesc(UUID planId);
}

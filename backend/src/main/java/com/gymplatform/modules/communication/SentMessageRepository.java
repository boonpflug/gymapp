package com.gymplatform.modules.communication;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.UUID;

@Repository
public interface SentMessageRepository extends JpaRepository<SentMessage, UUID> {

    Page<SentMessage> findByMemberIdOrderBySentAtDesc(UUID memberId, Pageable pageable);

    Page<SentMessage> findByStatusOrderBySentAtDesc(MessageStatus status, Pageable pageable);

    Page<SentMessage> findAllByOrderBySentAtDesc(Pageable pageable);

    @Query("SELECT COUNT(m) FROM SentMessage m WHERE m.status = :status AND m.sentAt >= :since")
    long countByStatusSince(@Param("status") MessageStatus status, @Param("since") Instant since);

    @Query("SELECT COUNT(m) FROM SentMessage m WHERE m.sentAt >= :since")
    long countSince(@Param("since") Instant since);
}

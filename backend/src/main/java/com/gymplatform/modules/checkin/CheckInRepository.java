package com.gymplatform.modules.checkin;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface CheckInRepository extends JpaRepository<CheckIn, UUID> {

    Page<CheckIn> findByMemberIdOrderByCheckInTimeDesc(UUID memberId, Pageable pageable);

    Page<CheckIn> findAllByOrderByCheckInTimeDesc(Pageable pageable);

    @Query("SELECT c FROM CheckIn c WHERE c.memberId = :memberId AND c.status = 'SUCCESS' " +
            "AND c.checkOutTime IS NULL AND c.checkInTime > :since ORDER BY c.checkInTime DESC")
    Optional<CheckIn> findActiveCheckIn(@Param("memberId") UUID memberId, @Param("since") Instant since);

    @Query("SELECT COUNT(c) FROM CheckIn c WHERE c.status = 'SUCCESS' AND c.checkOutTime IS NULL " +
            "AND c.checkInTime > :since")
    long countCurrentOccupancy(@Param("since") Instant since);

    @Query("SELECT c FROM CheckIn c WHERE c.status = 'SUCCESS' AND c.checkOutTime IS NULL " +
            "AND c.checkInTime > :since ORDER BY c.checkInTime DESC")
    Page<CheckIn> findCurrentlyCheckedIn(@Param("since") Instant since, Pageable pageable);
}

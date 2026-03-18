package com.gymplatform.modules.checkin;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface AccessEventRepository extends JpaRepository<AccessEvent, UUID> {

    Page<AccessEvent> findAllByOrderByCreatedAtDesc(Pageable pageable);

    Page<AccessEvent> findByDeviceIdOrderByCreatedAtDesc(UUID deviceId, Pageable pageable);

    Page<AccessEvent> findByMemberIdOrderByCreatedAtDesc(UUID memberId, Pageable pageable);
}

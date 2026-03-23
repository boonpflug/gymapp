package com.gymplatform.modules.machine;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface MachineSensorSessionRepository extends JpaRepository<MachineSensorSession, UUID> {
    Page<MachineSensorSession> findByMemberIdOrderByStartedAtDesc(UUID memberId, Pageable pageable);
    Page<MachineSensorSession> findByMachineIdOrderByStartedAtDesc(UUID machineId, Pageable pageable);
    List<MachineSensorSession> findByMemberIdAndMachineIdOrderByStartedAtDesc(UUID memberId, UUID machineId);
    List<MachineSensorSession> findByTrainingSessionId(UUID trainingSessionId);
}

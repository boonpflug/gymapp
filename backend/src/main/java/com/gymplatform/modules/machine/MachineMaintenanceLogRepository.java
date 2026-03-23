package com.gymplatform.modules.machine;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface MachineMaintenanceLogRepository extends JpaRepository<MachineMaintenanceLog, UUID> {
    Page<MachineMaintenanceLog> findByMachineIdOrderByPerformedAtDesc(UUID machineId, Pageable pageable);
    List<MachineMaintenanceLog> findByMaintenanceTypeOrderByPerformedAtDesc(MaintenanceType maintenanceType);
}

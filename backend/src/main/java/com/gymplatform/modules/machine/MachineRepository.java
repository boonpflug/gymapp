package com.gymplatform.modules.machine;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface MachineRepository extends JpaRepository<Machine, UUID> {
    List<Machine> findByFacilityIdOrderByCodeAsc(UUID facilityId);
    List<Machine> findByStatusOrderByCodeAsc(MachineStatus status);
    Optional<Machine> findByCode(String code);
    Optional<Machine> findBySerialNumber(String serialNumber);
    List<Machine> findByIsComputerAssistedTrueOrderByCodeAsc();
    List<Machine> findAllByOrderByCodeAsc();
}

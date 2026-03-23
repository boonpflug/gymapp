package com.gymplatform.modules.machine;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface StrengthMeasurementRepository extends JpaRepository<StrengthMeasurement, UUID> {
    Page<StrengthMeasurement> findByMemberIdOrderByMeasuredAtDesc(UUID memberId, Pageable pageable);
    List<StrengthMeasurement> findByMemberIdAndMachineIdOrderByMeasuredAtDesc(UUID memberId, UUID machineId);
    List<StrengthMeasurement> findBySensorSessionIdOrderBySetNumberAsc(UUID sensorSessionId);
    List<StrengthMeasurement> findByMemberIdAndMachineIdAndMeasurementTypeOrderByMeasuredAtAsc(UUID memberId, UUID machineId, MeasurementType measurementType);
}

package com.gymplatform.modules.contract;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface IdlePeriodRepository extends JpaRepository<IdlePeriod, UUID> {
    List<IdlePeriod> findByContractId(UUID contractId);

    @Query("SELECT ip FROM IdlePeriod ip WHERE ip.contractId = :contractId " +
           "AND ip.startDate <= :date AND ip.endDate >= :date")
    Optional<IdlePeriod> findActiveByContractId(UUID contractId, LocalDate date);
}

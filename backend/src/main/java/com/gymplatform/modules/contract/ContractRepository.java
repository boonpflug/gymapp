package com.gymplatform.modules.contract;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Repository
public interface ContractRepository extends JpaRepository<Contract, UUID> {
    List<Contract> findByMemberId(UUID memberId);
    List<Contract> findByStatus(ContractStatus status);
    List<Contract> findByNextBillingDateLessThanEqualAndStatus(LocalDate date, ContractStatus status);
    List<Contract> findByMemberIdAndStatus(UUID memberId, ContractStatus status);

    List<Contract> findByStatusAndAutoRenewTrueAndEndDateBetween(ContractStatus status, LocalDate start, LocalDate end);

    List<Contract> findByStatusAndAutoRenewTrueAndEndDateBefore(ContractStatus status, LocalDate date);
}

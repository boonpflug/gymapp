package com.gymplatform.modules.finance;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface DunningRunRepository extends JpaRepository<DunningRun, UUID> {
    Optional<DunningRun> findByInvoiceId(UUID invoiceId);
    List<DunningRun> findByResolvedFalse();
}

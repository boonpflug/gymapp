package com.gymplatform.modules.finance;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Repository
public interface InvoiceRepository extends JpaRepository<Invoice, UUID> {
    List<Invoice> findByMemberId(UUID memberId);
    Page<Invoice> findByStatus(InvoiceStatus status, Pageable pageable);
    List<Invoice> findByStatusIn(List<InvoiceStatus> statuses);
    List<Invoice> findByIssuedAtBetweenOrderByIssuedAtAsc(Instant from, Instant to);
}

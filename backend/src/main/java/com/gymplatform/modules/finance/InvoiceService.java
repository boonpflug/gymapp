package com.gymplatform.modules.finance;

import com.gymplatform.config.multitenancy.TenantContext;
import com.gymplatform.modules.finance.dto.InvoiceDto;
import com.gymplatform.shared.BusinessException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicLong;

@Slf4j
@Service
@RequiredArgsConstructor
public class InvoiceService {

    private final InvoiceRepository invoiceRepository;
    private static final AtomicLong invoiceCounter = new AtomicLong(10000);

    @Transactional
    public Invoice generateInvoice(UUID memberId, UUID contractId, BigDecimal amount) {
        BigDecimal vatAmount = amount.multiply(BigDecimal.valueOf(0.19));
        BigDecimal totalAmount = amount.add(vatAmount);

        Invoice invoice = Invoice.builder()
                .memberId(memberId)
                .contractId(contractId)
                .invoiceNumber("INV-" + invoiceCounter.incrementAndGet())
                .amount(amount)
                .vatAmount(vatAmount)
                .totalAmount(totalAmount)
                .currency("EUR")
                .status(InvoiceStatus.ISSUED)
                .issuedAt(Instant.now())
                .dueDate(LocalDate.now().plusDays(14))
                .tenantId(TenantContext.getTenantId())
                .build();

        invoice = invoiceRepository.save(invoice);
        log.info("Invoice generated: {} for member {}", invoice.getInvoiceNumber(), memberId);
        return invoice;
    }

    @Transactional
    public void markPaid(UUID invoiceId) {
        Invoice invoice = invoiceRepository.findById(invoiceId)
                .orElseThrow(() -> BusinessException.notFound("Invoice", invoiceId));
        invoice.setStatus(InvoiceStatus.PAID);
        invoice.setPaidAt(Instant.now());
        invoiceRepository.save(invoice);
    }

    @Transactional
    public void markOverdue(UUID invoiceId) {
        Invoice invoice = invoiceRepository.findById(invoiceId)
                .orElseThrow(() -> BusinessException.notFound("Invoice", invoiceId));
        invoice.setStatus(InvoiceStatus.OVERDUE);
        invoiceRepository.save(invoice);
    }

    public List<InvoiceDto> getByMember(UUID memberId) {
        return invoiceRepository.findByMemberId(memberId).stream()
                .map(this::toDto).toList();
    }

    public Page<InvoiceDto> getByStatus(InvoiceStatus status, Pageable pageable) {
        return invoiceRepository.findByStatus(status, pageable).map(this::toDto);
    }

    public InvoiceDto getInvoice(UUID id) {
        return toDto(invoiceRepository.findById(id)
                .orElseThrow(() -> BusinessException.notFound("Invoice", id)));
    }

    private InvoiceDto toDto(Invoice i) {
        return InvoiceDto.builder()
                .id(i.getId())
                .memberId(i.getMemberId())
                .contractId(i.getContractId())
                .invoiceNumber(i.getInvoiceNumber())
                .amount(i.getAmount())
                .vatAmount(i.getVatAmount())
                .totalAmount(i.getTotalAmount())
                .currency(i.getCurrency())
                .status(i.getStatus())
                .issuedAt(i.getIssuedAt())
                .dueDate(i.getDueDate())
                .paidAt(i.getPaidAt())
                .pdfUrl(i.getPdfUrl())
                .build();
    }
}

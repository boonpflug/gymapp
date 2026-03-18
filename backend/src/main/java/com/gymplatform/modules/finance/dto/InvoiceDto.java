package com.gymplatform.modules.finance.dto;

import com.gymplatform.modules.finance.InvoiceStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class InvoiceDto {
    private UUID id;
    private UUID memberId;
    private UUID contractId;
    private String invoiceNumber;
    private BigDecimal amount;
    private BigDecimal vatAmount;
    private BigDecimal totalAmount;
    private String currency;
    private InvoiceStatus status;
    private Instant issuedAt;
    private LocalDate dueDate;
    private Instant paidAt;
    private String pdfUrl;
}

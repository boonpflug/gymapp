package com.gymplatform.modules.finance;

import com.gymplatform.shared.BusinessException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class SwissLsvService {

    private final PaymentMethodRepository paymentMethodRepository;
    private final InvoiceRepository invoiceRepository;
    private final PaymentRepository paymentRepository;

    @Value("${swiss.lsv.subscriber-id:}")
    private String subscriberId;

    @Value("${swiss.lsv.bank-clearing:}")
    private String defaultBankClearing;

    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("yyyyMMdd");

    /**
     * Creates an LSV mandate record for a member, storing it in payment_methods with type LSV.
     */
    @Transactional
    public PaymentMethod createLsvMandate(UUID memberId, String iban, String bankClearing) {
        log.info("Creating LSV mandate for member {} with IBAN ending in {}", memberId,
                iban.length() >= 4 ? iban.substring(iban.length() - 4) : "****");

        // Validate Swiss IBAN format
        String cleanIban = iban.replaceAll("\\s+", "").toUpperCase();
        if (!cleanIban.startsWith("CH") && !cleanIban.startsWith("LI")) {
            throw new BusinessException("INVALID_IBAN",
                    "LSV requires a Swiss (CH) or Liechtenstein (LI) IBAN", HttpStatus.BAD_REQUEST);
        }
        if (cleanIban.length() != 21) {
            throw new BusinessException("INVALID_IBAN",
                    "Swiss IBAN must be exactly 21 characters", HttpStatus.BAD_REQUEST);
        }

        PaymentMethod mandate = PaymentMethod.builder()
                .memberId(memberId)
                .type(PaymentType.LSV)
                .lsvIban(cleanIban)
                .lsvBankClearing(bankClearing)
                .last4(cleanIban.substring(cleanIban.length() - 4))
                .isDefault(false)
                .active(true)
                .build();

        PaymentMethod saved = paymentMethodRepository.save(mandate);
        log.info("LSV mandate created with id {}", saved.getId());
        return saved;
    }

    /**
     * Generates an LSV+ batch collection file in TA 875 record format.
     *
     * Record types:
     * - Type 0: Header record (file identification)
     * - Type 1: Transaction records (one per invoice/debit)
     * - Type 9: Total record (summary)
     */
    public String generateLsvCollectionFile(LocalDate collectionDate, List<Invoice> invoices) {
        if (invoices.isEmpty()) {
            throw new BusinessException("NO_INVOICES",
                    "At least one invoice is required for LSV collection", HttpStatus.BAD_REQUEST);
        }

        log.info("Generating LSV collection file for {} invoices, collection date {}",
                invoices.size(), collectionDate);

        StringBuilder file = new StringBuilder();

        // Header record (Type 0)
        file.append(buildHeaderRecord(collectionDate));

        // Transaction records (Type 1)
        BigDecimal totalAmount = BigDecimal.ZERO;
        int transactionCount = 0;
        for (Invoice invoice : invoices) {
            // Find the LSV mandate for this member
            List<PaymentMethod> mandates = paymentMethodRepository
                    .findByMemberIdAndActiveTrue(invoice.getMemberId())
                    .stream()
                    .filter(pm -> pm.getType() == PaymentType.LSV)
                    .collect(Collectors.toList());

            if (mandates.isEmpty()) {
                log.warn("No active LSV mandate for member {}, skipping invoice {}",
                        invoice.getMemberId(), invoice.getInvoiceNumber());
                continue;
            }

            PaymentMethod mandate = mandates.get(0);
            file.append(buildTransactionRecord(invoice, mandate, collectionDate, transactionCount + 1));
            totalAmount = totalAmount.add(invoice.getTotalAmount());
            transactionCount++;
        }

        if (transactionCount == 0) {
            throw new BusinessException("NO_LSV_MANDATES",
                    "No active LSV mandates found for the given invoices", HttpStatus.BAD_REQUEST);
        }

        // Total record (Type 9)
        file.append(buildTotalRecord(totalAmount, transactionCount));

        log.info("LSV collection file generated: {} transactions, total amount {}",
                transactionCount, totalAmount);
        return file.toString();
    }

    /**
     * Parses an LSV return file (rejections/chargebacks) and updates payment statuses accordingly.
     */
    @Transactional
    public List<String> processLsvReturn(String returnFileContent) {
        log.info("Processing LSV return file");
        List<String> processedResults = new ArrayList<>();

        String[] lines = returnFileContent.split("\\r?\\n");
        for (String line : lines) {
            if (line.length() < 10) continue;

            String recordType = line.substring(0, 2).trim();
            if (!"1".equals(recordType) && !"01".equals(recordType)) {
                continue; // Only process transaction records
            }

            try {
                // Extract reference (invoice number) from the return record
                // Position depends on TA 875 return format — reference typically at position 80-106
                String reference = extractField(line, 80, 106).trim();
                // Extract rejection code
                String rejectionCode = extractField(line, 106, 110).trim();

                if (!rejectionCode.isEmpty() && !"0".equals(rejectionCode) && !"00".equals(rejectionCode)) {
                    // This is a rejection — find matching payment and mark as failed
                    log.info("LSV rejection for reference {}: code {}", reference, rejectionCode);

                    List<Invoice> matchingInvoices = invoiceRepository.findByStatusIn(
                            List.of(InvoiceStatus.ISSUED));
                    for (Invoice inv : matchingInvoices) {
                        if (inv.getInvoiceNumber() != null && inv.getInvoiceNumber().equals(reference)) {
                            inv.setStatus(InvoiceStatus.OVERDUE);
                            invoiceRepository.save(inv);
                            processedResults.add("REJECTED: " + reference + " (code: " + rejectionCode + ")");
                            break;
                        }
                    }
                } else {
                    processedResults.add("OK: " + reference);
                }
            } catch (Exception e) {
                log.warn("Could not parse LSV return line: {}", line, e);
                processedResults.add("PARSE_ERROR: " + line.substring(0, Math.min(20, line.length())));
            }
        }

        log.info("LSV return processing complete: {} results", processedResults.size());
        return processedResults;
    }

    /**
     * Lists all active LSV mandates for a given member.
     */
    public List<PaymentMethod> getLsvMandates(UUID memberId) {
        return paymentMethodRepository.findByMemberIdAndActiveTrue(memberId)
                .stream()
                .filter(pm -> pm.getType() == PaymentType.LSV)
                .collect(Collectors.toList());
    }

    // --- TA 875 Record Builders ---

    private String buildHeaderRecord(LocalDate collectionDate) {
        // TA 875 Type 0: Header
        // Format: record type (2) + subscriber ID (5) + sequence number (5) +
        //         creation date (8) + bank clearing (7) + processing date (8) + currency (3) + filler
        StringBuilder rec = new StringBuilder();
        rec.append("00");                                                    // Record type
        rec.append(padRight(subscriberId, 5));                               // LSV subscriber ID
        rec.append("00001");                                                 // Sequence number
        rec.append(LocalDate.now().format(DATE_FORMAT));                     // Creation date
        rec.append(padRight(defaultBankClearing, 7));                        // Bank clearing number
        rec.append(collectionDate.format(DATE_FORMAT));                      // Requested collection date
        rec.append("CHF");                                                   // Currency
        rec.append(padRight("", 46));                                        // Reserved / filler
        rec.append("\r\n");
        return rec.toString();
    }

    private String buildTransactionRecord(Invoice invoice, PaymentMethod mandate,
                                           LocalDate collectionDate, int sequenceNumber) {
        // TA 875 Type 1: Transaction
        StringBuilder rec = new StringBuilder();
        rec.append("01");                                                    // Record type
        rec.append(padRight(subscriberId, 5));                               // LSV subscriber ID
        rec.append(String.format("%05d", sequenceNumber));                   // Sequence number
        rec.append(padRight(mandate.getLsvBankClearing(), 7));               // Debtor bank clearing
        rec.append(padRight(mandate.getLsvIban(), 34));                      // Debtor IBAN
        rec.append(collectionDate.format(DATE_FORMAT));                      // Collection date
        rec.append(formatAmountForLsv(invoice.getTotalAmount()));            // Amount (12 chars, right-aligned)
        rec.append("CHF");                                                   // Currency
        rec.append(padRight(invoice.getInvoiceNumber(), 26));                // Reference
        rec.append(padRight("", 21));                                        // Reserved / filler
        rec.append("\r\n");
        return rec.toString();
    }

    private String buildTotalRecord(BigDecimal totalAmount, int transactionCount) {
        // TA 875 Type 9: Total
        StringBuilder rec = new StringBuilder();
        rec.append("09");                                                    // Record type
        rec.append(padRight(subscriberId, 5));                               // LSV subscriber ID
        rec.append(String.format("%05d", transactionCount));                 // Number of transactions
        rec.append(formatAmountForLsv(totalAmount));                         // Total amount
        rec.append("CHF");                                                   // Currency
        rec.append(padRight("", 55));                                        // Reserved / filler
        rec.append("\r\n");
        return rec.toString();
    }

    private String formatAmountForLsv(BigDecimal amount) {
        // Amount in cents, right-aligned, zero-padded, 12 characters
        long cents = amount.multiply(BigDecimal.valueOf(100)).longValue();
        return String.format("%012d", cents);
    }

    private String padRight(String value, int length) {
        if (value == null) value = "";
        if (value.length() >= length) return value.substring(0, length);
        return value + " ".repeat(length - value.length());
    }

    private String extractField(String line, int start, int end) {
        if (line.length() < end) {
            return line.length() > start ? line.substring(start) : "";
        }
        return line.substring(start, end);
    }
}

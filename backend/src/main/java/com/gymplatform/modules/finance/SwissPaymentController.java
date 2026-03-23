package com.gymplatform.modules.finance;

import com.gymplatform.modules.finance.dto.QrBillDto;
import com.gymplatform.shared.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/finance/swiss")
@RequiredArgsConstructor
public class SwissPaymentController {

    private final SwissQrBillService qrBillService;
    private final SwissLsvService lsvService;
    private final InvoiceRepository invoiceRepository;

    /**
     * Get QR-bill data for an invoice, ready for rendering as Swiss QR-bill payment slip.
     */
    @GetMapping("/qr-bill/{invoiceId}")
    @PreAuthorize("hasAnyRole('STUDIO_OWNER', 'MANAGER')")
    public ResponseEntity<ApiResponse<QrBillDto>> getQrBillData(@PathVariable UUID invoiceId) {
        QrBillDto data = qrBillService.getQrBillData(invoiceId);
        return ResponseEntity.ok(ApiResponse.success(data));
    }

    /**
     * Create an LSV direct debit mandate for a member.
     */
    @PostMapping("/lsv/mandates")
    @PreAuthorize("hasAnyRole('STUDIO_OWNER', 'MANAGER')")
    public ResponseEntity<ApiResponse<Map<String, Object>>> createLsvMandate(
            @RequestBody CreateLsvMandateRequest request) {
        PaymentMethod mandate = lsvService.createLsvMandate(
                request.memberId(), request.iban(), request.bankClearing());
        Map<String, Object> result = Map.of(
                "id", mandate.getId(),
                "memberId", mandate.getMemberId(),
                "type", mandate.getType().name(),
                "last4", mandate.getLast4(),
                "active", mandate.isActive()
        );
        return ResponseEntity.ok(ApiResponse.success(result));
    }

    /**
     * List active LSV mandates for a member.
     */
    @GetMapping("/lsv/mandates/{memberId}")
    @PreAuthorize("hasAnyRole('STUDIO_OWNER', 'MANAGER')")
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> getLsvMandates(
            @PathVariable UUID memberId) {
        List<PaymentMethod> mandates = lsvService.getLsvMandates(memberId);
        List<Map<String, Object>> result = mandates.stream()
                .map(m -> Map.<String, Object>of(
                        "id", m.getId(),
                        "memberId", m.getMemberId(),
                        "iban", maskIban(m.getLsvIban()),
                        "bankClearing", m.getLsvBankClearing() != null ? m.getLsvBankClearing() : "",
                        "last4", m.getLast4(),
                        "active", m.isActive()
                ))
                .collect(Collectors.toList());
        return ResponseEntity.ok(ApiResponse.success(result));
    }

    /**
     * Generate an LSV+ collection file for a batch of invoices.
     * Returns the file content as plain text (for download).
     */
    @PostMapping("/lsv/collection")
    @PreAuthorize("hasRole('STUDIO_OWNER')")
    public ResponseEntity<String> generateLsvCollection(
            @RequestBody LsvCollectionRequest request) {
        List<Invoice> invoices = request.invoiceIds().stream()
                .map(id -> invoiceRepository.findById(id)
                        .orElseThrow(() -> new com.gymplatform.shared.BusinessException(
                                "INVOICE_NOT_FOUND", "Invoice not found: " + id,
                                org.springframework.http.HttpStatus.NOT_FOUND)))
                .collect(Collectors.toList());

        String fileContent = lsvService.generateLsvCollectionFile(request.collectionDate(), invoices);
        return ResponseEntity.ok()
                .contentType(MediaType.TEXT_PLAIN)
                .header("Content-Disposition", "attachment; filename=lsv_collection_" +
                        request.collectionDate() + ".txt")
                .body(fileContent);
    }

    private String maskIban(String iban) {
        if (iban == null || iban.length() < 8) return "****";
        return iban.substring(0, 4) + "****" + iban.substring(iban.length() - 4);
    }

    // --- Request records ---

    record CreateLsvMandateRequest(UUID memberId, String iban, String bankClearing) {}

    record LsvCollectionRequest(LocalDate collectionDate, List<UUID> invoiceIds) {}
}

package com.gymplatform.modules.finance;

import com.gymplatform.modules.finance.dto.InvoiceDto;
import com.gymplatform.shared.ApiResponse;
import com.gymplatform.shared.PageMeta;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/invoices")
@PreAuthorize("hasAnyRole('STUDIO_OWNER', 'MANAGER')")
@RequiredArgsConstructor
public class InvoiceController {

    private final InvoiceService invoiceService;

    @GetMapping
    public ResponseEntity<ApiResponse<List<InvoiceDto>>> getInvoices(
            @RequestParam(required = false) InvoiceStatus status,
            Pageable pageable) {
        if (status != null) {
            Page<InvoiceDto> page = invoiceService.getByStatus(status, pageable);
            return ResponseEntity.ok(ApiResponse.success(page.getContent(), PageMeta.from(page)));
        }
        return ResponseEntity.ok(ApiResponse.success(List.of()));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<InvoiceDto>> getInvoice(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.success(invoiceService.getInvoice(id)));
    }

    @GetMapping("/member/{memberId}")
    public ResponseEntity<ApiResponse<List<InvoiceDto>>> getMemberInvoices(
            @PathVariable UUID memberId) {
        return ResponseEntity.ok(ApiResponse.success(invoiceService.getByMember(memberId)));
    }
}

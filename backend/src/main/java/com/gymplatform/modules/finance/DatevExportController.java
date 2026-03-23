package com.gymplatform.modules.finance;

import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.nio.charset.StandardCharsets;
import java.time.LocalDate;

@RestController
@RequestMapping("/api/finance/datev")
@PreAuthorize("hasAnyRole('STUDIO_OWNER', 'MANAGER')")
@RequiredArgsConstructor
public class DatevExportController {

    private final DatevExportService datevExportService;

    @GetMapping("/invoices")
    public ResponseEntity<byte[]> exportInvoices(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(defaultValue = "SKR03") String kontenrahmen) {

        String csv = datevExportService.exportInvoices(from, to, kontenrahmen);
        return buildCsvResponse(csv, "DATEV_Rechnungen_" + from + "_" + to + ".csv");
    }

    @GetMapping("/payments")
    public ResponseEntity<byte[]> exportPayments(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(defaultValue = "SKR03") String kontenrahmen) {

        String csv = datevExportService.exportPayments(from, to, kontenrahmen);
        return buildCsvResponse(csv, "DATEV_Zahlungen_" + from + "_" + to + ".csv");
    }

    @GetMapping("/full")
    public ResponseEntity<byte[]> exportFull(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(defaultValue = "SKR03") String kontenrahmen) {

        String csv = datevExportService.exportFull(from, to, kontenrahmen);
        return buildCsvResponse(csv, "DATEV_Export_" + from + "_" + to + ".csv");
    }

    private ResponseEntity<byte[]> buildCsvResponse(String csv, String filename) {
        byte[] bytes = csv.getBytes(StandardCharsets.UTF_8);
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.parseMediaType("text/csv; charset=UTF-8"));
        headers.setContentDispositionFormData("attachment", filename);
        headers.setContentLength(bytes.length);

        return ResponseEntity.ok()
                .headers(headers)
                .body(bytes);
    }
}

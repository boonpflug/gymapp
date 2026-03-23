package com.gymplatform.modules.finance;

import com.gymplatform.modules.member.Member;
import com.gymplatform.modules.member.MemberRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class DatevExportService {

    private static final ZoneId BERLIN = ZoneId.of("Europe/Berlin");
    private static final String DELIMITER = ";";
    private static final String DATEV_FORMAT_NAME = "EXTF";
    private static final int DATEV_FORMAT_VERSION = 700;
    private static final int DATEV_FORMAT_CATEGORY = 21; // Buchungsstapel
    private static final String DATEV_FORMAT_LABEL = "Buchungsstapel";

    // SKR03 accounts
    private static final int SKR03_REVENUE_19 = 8400;    // Erloese 19% USt
    private static final int SKR03_RECEIVABLES = 1400;    // Forderungen aus L+L
    private static final int SKR03_BANK = 1200;           // Bank

    // SKR04 accounts
    private static final int SKR04_REVENUE_19 = 4400;     // Erloese 19% USt
    private static final int SKR04_RECEIVABLES = 1200;    // Forderungen aus L+L
    private static final int SKR04_BANK = 1800;           // Bank

    private static final Map<String, int[]> ACCOUNT_MAP = Map.of(
            "SKR03", new int[]{SKR03_REVENUE_19, SKR03_RECEIVABLES, SKR03_BANK},
            "SKR04", new int[]{SKR04_REVENUE_19, SKR04_RECEIVABLES, SKR04_BANK}
    );

    private final InvoiceRepository invoiceRepository;
    private final PaymentRepository paymentRepository;
    private final MemberRepository memberRepository;

    /**
     * Exports all invoices in the given date range as a DATEV Buchungsstapel CSV.
     * Each invoice produces two booking lines:
     *   1) Debit receivables (Soll)
     *   2) Credit revenue (Haben)
     */
    public String exportInvoices(LocalDate from, LocalDate to, String kontenrahmen) {
        validateKontenrahmen(kontenrahmen);
        int[] accounts = ACCOUNT_MAP.get(kontenrahmen);
        int revenueAccount = accounts[0];
        int receivablesAccount = accounts[1];

        Instant fromInstant = from.atStartOfDay(BERLIN).toInstant();
        Instant toInstant = to.plusDays(1).atStartOfDay(BERLIN).toInstant();

        List<Invoice> invoices = invoiceRepository
                .findByIssuedAtBetweenOrderByIssuedAtAsc(fromInstant, toInstant);

        StringBuilder csv = new StringBuilder();
        csv.append(getDatevHeader(kontenrahmen, from, to));
        csv.append(getColumnHeader());

        for (Invoice inv : invoices) {
            String memberName = lookupMemberName(inv.getMemberId());
            String belegdatum = formatBelegdatum(inv.getIssuedAt());
            BigDecimal totalAmount = inv.getTotalAmount();
            String amountStr = formatDecimal(totalAmount);

            // Line 1: Debit receivables (Soll)
            csv.append(buildLine(
                    amountStr, "S", inv.getCurrency(),
                    receivablesAccount, revenueAccount,
                    "0", belegdatum,
                    inv.getInvoiceNumber(), "",
                    memberName
            ));

            // Line 2: Credit revenue (Haben)
            csv.append(buildLine(
                    amountStr, "H", inv.getCurrency(),
                    revenueAccount, receivablesAccount,
                    "0", belegdatum,
                    inv.getInvoiceNumber(), "",
                    "Mitgliedsbeitrag " + memberName
            ));
        }

        log.info("DATEV invoice export: {} invoices, range {} to {}, {}", invoices.size(), from, to, kontenrahmen);
        return csv.toString();
    }

    /**
     * Exports all payments in the given date range as a DATEV Buchungsstapel CSV.
     * Each payment produces two booking lines:
     *   1) Debit bank (Soll)
     *   2) Credit receivables (Haben)
     */
    public String exportPayments(LocalDate from, LocalDate to, String kontenrahmen) {
        validateKontenrahmen(kontenrahmen);
        int[] accounts = ACCOUNT_MAP.get(kontenrahmen);
        int receivablesAccount = accounts[1];
        int bankAccount = accounts[2];

        Instant fromInstant = from.atStartOfDay(BERLIN).toInstant();
        Instant toInstant = to.plusDays(1).atStartOfDay(BERLIN).toInstant();

        List<Payment> payments = paymentRepository
                .findByProcessedAtBetweenOrderByProcessedAtAsc(fromInstant, toInstant);

        StringBuilder csv = new StringBuilder();
        csv.append(getDatevHeader(kontenrahmen, from, to));
        csv.append(getColumnHeader());

        for (Payment pmt : payments) {
            String memberName = lookupMemberNameByInvoice(pmt.getInvoiceId());
            String invoiceNumber = lookupInvoiceNumber(pmt.getInvoiceId());
            String belegdatum = formatBelegdatum(pmt.getProcessedAt());
            String amountStr = formatDecimal(pmt.getAmount());

            // Line 1: Debit bank (Soll)
            csv.append(buildLine(
                    amountStr, "S", pmt.getCurrency(),
                    bankAccount, receivablesAccount,
                    "0", belegdatum,
                    invoiceNumber, "",
                    "Zahlung " + memberName
            ));

            // Line 2: Credit receivables (Haben)
            csv.append(buildLine(
                    amountStr, "H", pmt.getCurrency(),
                    receivablesAccount, bankAccount,
                    "0", belegdatum,
                    invoiceNumber, "",
                    "Zahlung " + memberName
            ));
        }

        log.info("DATEV payment export: {} payments, range {} to {}, {}", payments.size(), from, to, kontenrahmen);
        return csv.toString();
    }

    /**
     * Combined export: invoices followed by payments in one file.
     */
    public String exportFull(LocalDate from, LocalDate to, String kontenrahmen) {
        validateKontenrahmen(kontenrahmen);
        int[] accounts = ACCOUNT_MAP.get(kontenrahmen);
        int revenueAccount = accounts[0];
        int receivablesAccount = accounts[1];
        int bankAccount = accounts[2];

        Instant fromInstant = from.atStartOfDay(BERLIN).toInstant();
        Instant toInstant = to.plusDays(1).atStartOfDay(BERLIN).toInstant();

        StringBuilder csv = new StringBuilder();
        csv.append(getDatevHeader(kontenrahmen, from, to));
        csv.append(getColumnHeader());

        // Invoices
        List<Invoice> invoices = invoiceRepository
                .findByIssuedAtBetweenOrderByIssuedAtAsc(fromInstant, toInstant);

        for (Invoice inv : invoices) {
            String memberName = lookupMemberName(inv.getMemberId());
            String belegdatum = formatBelegdatum(inv.getIssuedAt());
            String amountStr = formatDecimal(inv.getTotalAmount());

            csv.append(buildLine(
                    amountStr, "S", inv.getCurrency(),
                    receivablesAccount, revenueAccount,
                    "0", belegdatum,
                    inv.getInvoiceNumber(), "",
                    memberName
            ));

            csv.append(buildLine(
                    amountStr, "H", inv.getCurrency(),
                    revenueAccount, receivablesAccount,
                    "0", belegdatum,
                    inv.getInvoiceNumber(), "",
                    "Mitgliedsbeitrag " + memberName
            ));
        }

        // Payments
        List<Payment> payments = paymentRepository
                .findByProcessedAtBetweenOrderByProcessedAtAsc(fromInstant, toInstant);

        for (Payment pmt : payments) {
            String memberName = lookupMemberNameByInvoice(pmt.getInvoiceId());
            String invoiceNumber = lookupInvoiceNumber(pmt.getInvoiceId());
            String belegdatum = formatBelegdatum(pmt.getProcessedAt());
            String amountStr = formatDecimal(pmt.getAmount());

            csv.append(buildLine(
                    amountStr, "S", pmt.getCurrency(),
                    bankAccount, receivablesAccount,
                    "0", belegdatum,
                    invoiceNumber, "",
                    "Zahlung " + memberName
            ));

            csv.append(buildLine(
                    amountStr, "H", pmt.getCurrency(),
                    receivablesAccount, bankAccount,
                    "0", belegdatum,
                    invoiceNumber, "",
                    "Zahlung " + memberName
            ));
        }

        log.info("DATEV full export: {} invoices + {} payments, range {} to {}, {}",
                invoices.size(), payments.size(), from, to, kontenrahmen);
        return csv.toString();
    }

    /**
     * Generates the DATEV header row (line 1 of the file).
     * Format: EXTF;700;21;"Buchungsstapel";...
     */
    public String getDatevHeader(String kontenrahmen, LocalDate from, LocalDate to) {
        // DATEV header line format (simplified but valid for import):
        // EXTF;Version;Category;"Label";Version;"GeneratedBy";
        // "";"";"";Consultant;Client;FiscalYearStart;AccountLength;
        // DateFrom;DateTo;Description;"";""
        int fiscalYear = from.getYear();
        String dateFrom = from.format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        String dateTo = to.format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        LocalDate fiscalYearStart = LocalDate.of(fiscalYear, 1, 1);
        String fiscalStart = fiscalYearStart.format(DateTimeFormatter.ofPattern("yyyyMMdd"));

        return String.join(DELIMITER,
                "\"" + DATEV_FORMAT_NAME + "\"",                 // Format
                String.valueOf(DATEV_FORMAT_VERSION),            // Version
                String.valueOf(DATEV_FORMAT_CATEGORY),           // Category
                "\"" + DATEV_FORMAT_LABEL + "\"",                // Label
                "12",                                             // Format version
                "\"\"",                                           // Created by (generated)
                "\"\"",                                           // Reserved
                "\"" + kontenrahmen + "\"",                      // Reserved / Kontenrahmen
                "\"\"",                                           // Reserved
                "10000",                                          // Berater-Nr (consultant)
                "10001",                                          // Mandanten-Nr (client)
                fiscalStart,                                      // Fiscal year start
                "4",                                              // Account number length
                dateFrom,                                         // Date from
                dateTo,                                           // Date to
                "\"GymPlatform Export\"",                         // Description
                "\"\"",                                           // Diktat-Kuerzel
                "\"\"",                                           // Reserved
                "\"\"" )                                          // Reserved
                + "\r\n";
    }

    // ----- private helpers -----

    private String getColumnHeader() {
        return String.join(DELIMITER,
                "Umsatz (ohne Soll/Haben-Kz)",
                "Soll/Haben-Kennzeichen",
                "WKZ Umsatz",
                "Kurs",
                "Basis-Umsatz",
                "WKZ Basis-Umsatz",
                "Konto",
                "Gegenkonto (ohne BU-Schluessel)",
                "BU-Schluessel",
                "Belegdatum",
                "Belegfeld 1",
                "Belegfeld 2",
                "Skonto",
                "Buchungstext"
        ) + "\r\n";
    }

    private String buildLine(String amount, String sollHaben, String currency,
                             int konto, int gegenkonto, String buSchluessel,
                             String belegdatum, String belegfeld1, String belegfeld2,
                             String buchungstext) {
        // Truncate Buchungstext to 60 chars (DATEV limit)
        if (buchungstext != null && buchungstext.length() > 60) {
            buchungstext = buchungstext.substring(0, 60);
        }
        return String.join(DELIMITER,
                amount,                                       // Umsatz
                sollHaben,                                    // Soll/Haben
                "\"" + currency + "\"",                       // WKZ
                "",                                           // Kurs (empty for EUR)
                "",                                           // Basis-Umsatz
                "",                                           // WKZ Basis-Umsatz
                String.valueOf(konto),                        // Konto
                String.valueOf(gegenkonto),                   // Gegenkonto
                buSchluessel,                                 // BU-Schluessel
                belegdatum,                                   // Belegdatum (DDMM)
                "\"" + nullSafe(belegfeld1) + "\"",           // Belegfeld 1
                "\"" + nullSafe(belegfeld2) + "\"",           // Belegfeld 2
                "",                                           // Skonto
                "\"" + nullSafe(buchungstext) + "\""          // Buchungstext
        ) + "\r\n";
    }

    private String formatDecimal(BigDecimal value) {
        // DATEV uses comma as decimal separator: "49,90"
        if (value == null) return "0,00";
        return value.setScale(2).toPlainString().replace('.', ',');
    }

    private String formatBelegdatum(Instant instant) {
        // DATEV Belegdatum format: DDMM (e.g., "1503" for March 15)
        if (instant == null) return "";
        return instant.atZone(BERLIN).format(DateTimeFormatter.ofPattern("ddMM"));
    }

    private String lookupMemberName(UUID memberId) {
        if (memberId == null) return "Unbekannt";
        Optional<Member> member = memberRepository.findById(memberId);
        return member.map(m -> m.getLastName() + ", " + m.getFirstName())
                .orElse("Mitglied " + memberId.toString().substring(0, 8));
    }

    private String lookupMemberNameByInvoice(UUID invoiceId) {
        if (invoiceId == null) return "Unbekannt";
        return invoiceRepository.findById(invoiceId)
                .map(inv -> lookupMemberName(inv.getMemberId()))
                .orElse("Unbekannt");
    }

    private String lookupInvoiceNumber(UUID invoiceId) {
        if (invoiceId == null) return "";
        return invoiceRepository.findById(invoiceId)
                .map(Invoice::getInvoiceNumber)
                .orElse("");
    }

    private String nullSafe(String s) {
        return s == null ? "" : s;
    }

    private void validateKontenrahmen(String kontenrahmen) {
        if (kontenrahmen == null || !ACCOUNT_MAP.containsKey(kontenrahmen)) {
            throw new IllegalArgumentException(
                    "Unsupported Kontenrahmen: " + kontenrahmen + ". Use SKR03 or SKR04.");
        }
    }
}

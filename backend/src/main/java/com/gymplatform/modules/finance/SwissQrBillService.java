package com.gymplatform.modules.finance;

import com.gymplatform.modules.finance.dto.QrBillDto;
import com.gymplatform.shared.BusinessException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class SwissQrBillService {

    private final InvoiceRepository invoiceRepository;

    @Value("${swiss.qr-bill.creditor-iban}")
    private String creditorIban;

    @Value("${swiss.qr-bill.creditor-name}")
    private String creditorName;

    @Value("${swiss.qr-bill.creditor-street}")
    private String creditorStreet;

    @Value("${swiss.qr-bill.creditor-city}")
    private String creditorCity;

    @Value("${swiss.qr-bill.creditor-postal-code}")
    private String creditorPostalCode;

    private static final String CRLF = "\r\n";

    /**
     * Builds the Swiss QR-bill payment data string (SPC format, version 0200).
     * Conforms to the Swiss Payment Standards "Swiss QR Code" specification.
     */
    public String generateQrPaymentData(Invoice invoice, String creditorIban, String creditorName,
                                         String creditorStreet, String creditorCity,
                                         String creditorPostalCode) {
        String reference = generateQrReference(invoice.getInvoiceNumber());
        BigDecimal amount = invoice.getTotalAmount();

        StringBuilder sb = new StringBuilder();
        // Header
        sb.append("SPC").append(CRLF);                              // QRType
        sb.append("0200").append(CRLF);                             // Version
        sb.append("1").append(CRLF);                                // Coding (UTF-8)
        // Creditor account
        sb.append(formatIban(creditorIban)).append(CRLF);           // IBAN
        // Creditor (S = structured address)
        sb.append("S").append(CRLF);                                // Address type
        sb.append(creditorName).append(CRLF);                       // Name
        sb.append(creditorStreet).append(CRLF);                     // Street or address line 1
        sb.append("").append(CRLF);                                 // Building number or address line 2
        sb.append(creditorPostalCode).append(CRLF);                 // Postal code
        sb.append(creditorCity).append(CRLF);                       // City
        sb.append("CH").append(CRLF);                               // Country
        // Ultimate creditor (empty — not used)
        sb.append("").append(CRLF);                                 // UCr address type
        sb.append("").append(CRLF);                                 // UCr name
        sb.append("").append(CRLF);                                 // UCr street
        sb.append("").append(CRLF);                                 // UCr building number
        sb.append("").append(CRLF);                                 // UCr postal code
        sb.append("").append(CRLF);                                 // UCr city
        sb.append("").append(CRLF);                                 // UCr country
        // Payment amount information
        sb.append(formatAmount(amount)).append(CRLF);               // Amount
        sb.append("CHF").append(CRLF);                              // Currency
        // Ultimate debtor (empty — filled by payer)
        sb.append("").append(CRLF);                                 // Debtor address type
        sb.append("").append(CRLF);                                 // Debtor name
        sb.append("").append(CRLF);                                 // Debtor street
        sb.append("").append(CRLF);                                 // Debtor building number
        sb.append("").append(CRLF);                                 // Debtor postal code
        sb.append("").append(CRLF);                                 // Debtor city
        sb.append("").append(CRLF);                                 // Debtor country
        // Reference
        sb.append("QRR").append(CRLF);                              // Reference type
        sb.append(reference).append(CRLF);                          // Reference
        // Additional information
        sb.append("Invoice " + invoice.getInvoiceNumber()).append(CRLF); // Unstructured message
        sb.append("EPD");                                           // Trailer

        return sb.toString();
    }

    /**
     * Generates a 26-digit QR reference from the invoice number using mod 10 recursive check digit.
     * The invoice number is zero-padded to 25 digits, then a check digit is appended.
     */
    public String generateQrReference(String invoiceNumber) {
        // Strip non-numeric characters
        String numeric = invoiceNumber.replaceAll("[^0-9]", "");
        // Pad to 25 digits
        String padded = String.format("%25s", numeric).replace(' ', '0');
        if (padded.length() > 25) {
            padded = padded.substring(padded.length() - 25);
        }
        int checkDigit = calculateMod10Recursive(padded);
        return padded + checkDigit;
    }

    /**
     * Generates an ISO 11649 Creditor Reference (RF + 2 check digits + reference).
     */
    public String generateCreditorReference(String invoiceNumber) {
        String numeric = invoiceNumber.replaceAll("[^0-9A-Za-z]", "");
        // For ISO 11649: calculate check digits
        // Convert reference + "RF00" to numeric representation for mod 97
        String refWithRf = numeric + "RF00";
        StringBuilder numericRepr = new StringBuilder();
        for (char c : refWithRf.toCharArray()) {
            if (Character.isDigit(c)) {
                numericRepr.append(c);
            } else {
                numericRepr.append(Character.toUpperCase(c) - 'A' + 10);
            }
        }
        // Calculate mod 97
        int remainder = mod97(numericRepr.toString());
        int checkDigits = 98 - remainder;
        return String.format("RF%02d%s", checkDigits, numeric);
    }

    /**
     * Returns a QrBillDto with all QR-bill fields ready for rendering, for the given invoice.
     */
    public QrBillDto getQrBillData(UUID invoiceId) {
        Invoice invoice = invoiceRepository.findById(invoiceId)
                .orElseThrow(() -> new BusinessException("INVOICE_NOT_FOUND",
                        "Invoice not found: " + invoiceId, HttpStatus.NOT_FOUND));

        String qrReference = generateQrReference(invoice.getInvoiceNumber());
        String qrPayload = generateQrPaymentData(invoice, creditorIban, creditorName,
                creditorStreet, creditorCity, creditorPostalCode);

        String creditorAddress = creditorStreet + ", " + creditorPostalCode + " " + creditorCity;

        return QrBillDto.builder()
                .creditorIban(formatIban(creditorIban))
                .creditorName(creditorName)
                .creditorAddress(creditorAddress)
                .amount(formatAmount(invoice.getTotalAmount()))
                .currency("CHF")
                .debtorName("")
                .debtorAddress("")
                .referenceType("QRR")
                .reference(qrReference)
                .additionalInfo("Invoice " + invoice.getInvoiceNumber())
                .qrPayload(qrPayload)
                .invoiceNumber(invoice.getInvoiceNumber())
                .build();
    }

    /**
     * Mod 10 recursive algorithm used for Swiss QR reference check digits.
     */
    private int calculateMod10Recursive(String input) {
        int[] table = {0, 9, 4, 6, 8, 2, 7, 1, 3, 5};
        int carry = 0;
        for (int i = 0; i < input.length(); i++) {
            int digit = Character.getNumericValue(input.charAt(i));
            carry = table[(carry + digit) % 10];
        }
        return (10 - carry) % 10;
    }

    /**
     * Calculates mod 97 for large numeric strings (used for ISO 11649).
     */
    private int mod97(String numericString) {
        int remainder = 0;
        for (int i = 0; i < numericString.length(); i++) {
            int digit = Character.getNumericValue(numericString.charAt(i));
            remainder = (remainder * 10 + digit) % 97;
        }
        return remainder;
    }

    private String formatIban(String iban) {
        return iban.replaceAll("\\s+", "").toUpperCase();
    }

    private String formatAmount(BigDecimal amount) {
        return amount.setScale(2).toPlainString();
    }
}

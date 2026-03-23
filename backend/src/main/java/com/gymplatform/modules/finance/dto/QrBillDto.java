package com.gymplatform.modules.finance.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class QrBillDto {
    private String creditorIban;
    private String creditorName;
    private String creditorAddress;
    private String amount;
    private String currency;
    private String debtorName;
    private String debtorAddress;
    private String referenceType;
    private String reference;
    private String additionalInfo;
    private String qrPayload;
    private String invoiceNumber;
}

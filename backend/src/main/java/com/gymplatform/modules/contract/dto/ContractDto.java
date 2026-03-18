package com.gymplatform.modules.contract.dto;

import com.gymplatform.modules.contract.ContractStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class ContractDto {
    private UUID id;
    private UUID memberId;
    private UUID membershipTierId;
    private String membershipTierName;
    private ContractStatus status;
    private LocalDate startDate;
    private LocalDate endDate;
    private LocalDate nextBillingDate;
    private BigDecimal monthlyAmount;
    private String discountCode;
    private LocalDate cancellationDate;
    private LocalDate cancellationEffectiveDate;
    private String cancellationReason;
    private boolean autoRenew;
}

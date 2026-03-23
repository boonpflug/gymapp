package com.gymplatform.modules.loyalty.dto;

import com.gymplatform.modules.loyalty.LoyaltyAction;
import com.gymplatform.modules.loyalty.TransactionType;
import lombok.Builder;
import lombok.Data;

import java.time.Instant;
import java.util.UUID;

@Data
@Builder
public class LoyaltyTransactionDto {
    private UUID id;
    private UUID memberId;
    private String memberName;
    private int points;
    private int balanceAfter;
    private TransactionType transactionType;
    private LoyaltyAction action;
    private UUID referenceId;
    private String description;
    private Instant createdAt;
}

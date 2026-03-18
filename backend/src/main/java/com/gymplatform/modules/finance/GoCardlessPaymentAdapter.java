package com.gymplatform.modules.finance;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

@Slf4j
@Service
public class GoCardlessPaymentAdapter {

    @Value("${gocardless.access-token}")
    private String accessToken;

    @Value("${gocardless.environment}")
    private String environment;

    public String createPayment(String mandateId, BigDecimal amount, String currency,
                                String description) {
        log.info("Creating GoCardless payment: mandate={}, amount={} {}, desc={}",
                mandateId, amount, currency, description);
        // GoCardless API integration - returns payment ID
        // In production, use the GoCardless Java SDK
        return "GC-PAY-" + System.currentTimeMillis();
    }

    public String getPaymentStatus(String paymentId) {
        log.info("Checking GoCardless payment status: {}", paymentId);
        return "confirmed";
    }
}

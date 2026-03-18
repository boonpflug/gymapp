package com.gymplatform.modules.finance;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/webhooks/gocardless")
@RequiredArgsConstructor
public class GoCardlessWebhookController {

    private final PaymentRepository paymentRepository;
    private final InvoiceService invoiceService;

    @PostMapping
    public ResponseEntity<Void> handleWebhook(
            @RequestHeader("Webhook-Signature") String signature,
            @RequestBody Map<String, Object> payload) {

        log.info("Received GoCardless webhook");

        // In production, verify the webhook signature
        @SuppressWarnings("unchecked")
        var events = (java.util.List<Map<String, Object>>) payload.get("events");
        if (events == null) {
            return ResponseEntity.ok().build();
        }

        for (Map<String, Object> event : events) {
            String resourceType = (String) event.get("resource_type");
            String action = (String) event.get("action");

            if ("payments".equals(resourceType)) {
                @SuppressWarnings("unchecked")
                var links = (Map<String, String>) event.get("links");
                String paymentId = links != null ? links.get("payment") : null;

                if (paymentId != null) {
                    handlePaymentEvent(paymentId, action);
                }
            }
        }

        return ResponseEntity.ok().build();
    }

    private void handlePaymentEvent(String gcPaymentId, String action) {
        paymentRepository.findByGoCardlessPaymentId(gcPaymentId).ifPresent(payment -> {
            switch (action) {
                case "confirmed" -> {
                    payment.setStatus(PaymentStatus.SUCCEEDED);
                    payment.setProcessedAt(Instant.now());
                    paymentRepository.save(payment);
                    if (payment.getInvoiceId() != null) {
                        invoiceService.markPaid(payment.getInvoiceId());
                    }
                    log.info("GoCardless payment confirmed: {}", gcPaymentId);
                }
                case "failed" -> {
                    payment.setStatus(PaymentStatus.FAILED);
                    payment.setFailureReason("GoCardless payment failed");
                    paymentRepository.save(payment);
                    log.warn("GoCardless payment failed: {}", gcPaymentId);
                }
                case "charged_back" -> {
                    payment.setStatus(PaymentStatus.FAILED);
                    payment.setFailureReason("GoCardless payment charged back");
                    paymentRepository.save(payment);
                    if (payment.getInvoiceId() != null) {
                        invoiceService.markOverdue(payment.getInvoiceId());
                    }
                    log.warn("GoCardless payment charged back: {}", gcPaymentId);
                }
                default -> log.info("Unhandled GoCardless payment action: {}", action);
            }
        });
    }
}

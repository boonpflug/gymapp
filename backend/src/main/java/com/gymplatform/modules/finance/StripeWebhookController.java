package com.gymplatform.modules.finance;

import com.stripe.model.Event;
import com.stripe.model.PaymentIntent;
import com.stripe.net.Webhook;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;

@Slf4j
@RestController
@RequestMapping("/api/webhooks/stripe")
@RequiredArgsConstructor
public class StripeWebhookController {

    private final PaymentRepository paymentRepository;
    private final InvoiceService invoiceService;

    @Value("${stripe.webhook-secret}")
    private String webhookSecret;

    @PostMapping
    public ResponseEntity<String> handleWebhook(
            @RequestHeader("Stripe-Signature") String sigHeader,
            @RequestBody String payload) {

        Event event;
        try {
            event = Webhook.constructEvent(payload, sigHeader, webhookSecret);
        } catch (Exception e) {
            log.error("Stripe webhook signature verification failed", e);
            return ResponseEntity.badRequest().body("Invalid signature");
        }

        log.info("Stripe webhook received: {}", event.getType());

        switch (event.getType()) {
            case "payment_intent.succeeded" -> {
                PaymentIntent intent = (PaymentIntent) event.getDataObjectDeserializer()
                        .getObject().orElse(null);
                if (intent != null) {
                    handlePaymentSucceeded(intent.getId());
                }
            }
            case "payment_intent.payment_failed" -> {
                PaymentIntent intent = (PaymentIntent) event.getDataObjectDeserializer()
                        .getObject().orElse(null);
                if (intent != null) {
                    handlePaymentFailed(intent.getId(),
                            intent.getLastPaymentError() != null
                                    ? intent.getLastPaymentError().getMessage()
                                    : "Unknown error");
                }
            }
            default -> log.info("Unhandled Stripe event type: {}", event.getType());
        }

        return ResponseEntity.ok("ok");
    }

    private void handlePaymentSucceeded(String paymentIntentId) {
        paymentRepository.findByStripePaymentIntentId(paymentIntentId).ifPresent(payment -> {
            payment.setStatus(PaymentStatus.SUCCEEDED);
            payment.setProcessedAt(Instant.now());
            paymentRepository.save(payment);
            if (payment.getInvoiceId() != null) {
                invoiceService.markPaid(payment.getInvoiceId());
            }
            log.info("Stripe payment succeeded: {}", paymentIntentId);
        });
    }

    private void handlePaymentFailed(String paymentIntentId, String reason) {
        paymentRepository.findByStripePaymentIntentId(paymentIntentId).ifPresent(payment -> {
            payment.setStatus(PaymentStatus.FAILED);
            payment.setFailureReason(reason);
            paymentRepository.save(payment);
            log.warn("Stripe payment failed: {} - {}", paymentIntentId, reason);
        });
    }
}

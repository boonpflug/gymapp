package com.gymplatform.modules.finance;

import com.gymplatform.config.multitenancy.TenantContext;
import com.gymplatform.shared.BusinessException;
import com.stripe.model.PaymentIntent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class PaymentService {

    private final PaymentRepository paymentRepository;
    private final PaymentMethodRepository paymentMethodRepository;
    private final InvoiceService invoiceService;
    private final StripePaymentAdapter stripeAdapter;
    private final GoCardlessPaymentAdapter goCardlessAdapter;
    private final RabbitTemplate rabbitTemplate;

    @Transactional
    public Payment processPayment(Invoice invoice, PaymentMethod paymentMethod) {
        Payment payment = Payment.builder()
                .invoiceId(invoice.getId())
                .paymentMethodId(paymentMethod.getId())
                .amount(invoice.getTotalAmount())
                .currency(invoice.getCurrency())
                .status(PaymentStatus.PENDING)
                .tenantId(TenantContext.getTenantId())
                .build();

        try {
            if (paymentMethod.getType() == PaymentType.CREDIT_CARD) {
                PaymentIntent intent = stripeAdapter.createPaymentIntent(
                        invoice.getTotalAmount(),
                        invoice.getCurrency(),
                        paymentMethod.getStripePaymentMethodId(),
                        "Invoice " + invoice.getInvoiceNumber()
                );
                payment.setStripePaymentIntentId(intent.getId());
                if ("succeeded".equals(intent.getStatus())) {
                    payment.setStatus(PaymentStatus.SUCCEEDED);
                    payment.setProcessedAt(Instant.now());
                    invoiceService.markPaid(invoice.getId());
                }
            } else if (paymentMethod.getType() == PaymentType.SEPA_DIRECT_DEBIT) {
                String gcPaymentId = goCardlessAdapter.createPayment(
                        paymentMethod.getGoCardlessMandateId(),
                        invoice.getTotalAmount(),
                        invoice.getCurrency(),
                        "Invoice " + invoice.getInvoiceNumber()
                );
                payment.setGoCardlessPaymentId(gcPaymentId);
                payment.setStatus(PaymentStatus.PENDING);
            } else if (paymentMethod.getType() == PaymentType.CASH) {
                payment.setStatus(PaymentStatus.SUCCEEDED);
                payment.setProcessedAt(Instant.now());
                invoiceService.markPaid(invoice.getId());
            }
        } catch (Exception e) {
            log.error("Payment processing failed for invoice {}: {}",
                    invoice.getInvoiceNumber(), e.getMessage());
            payment.setStatus(PaymentStatus.FAILED);
            payment.setFailureReason(e.getMessage());

            try {
                rabbitTemplate.convertAndSend("payment.events", "payment.failed",
                        invoice.getId().toString());
            } catch (Exception ex) {
                log.warn("Failed to publish payment.failed event", ex);
            }
        }

        return paymentRepository.save(payment);
    }

    public Payment getPayment(UUID id) {
        return paymentRepository.findById(id)
                .orElseThrow(() -> BusinessException.notFound("Payment", id));
    }
}

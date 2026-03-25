package com.gymplatform.modules.finance;

import com.gymplatform.shared.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

import java.util.UUID;

@Entity
@Table(name = "payment_methods")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class PaymentMethod extends BaseEntity {

    @Column(name = "member_id", nullable = false)
    private UUID memberId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PaymentType type;

    @Column(name = "stripe_payment_method_id")
    private String stripePaymentMethodId;

    @Column(name = "gocardless_mandate_id")
    private String goCardlessMandateId;

    @Column(name = "lsv_iban")
    private String lsvIban;

    @Column(name = "lsv_bank_clearing")
    private String lsvBankClearing;

    @Column(name = "last4")
    private String last4;

    @Column(name = "is_default")
    private boolean isDefault;

    @Column(nullable = false)
    private boolean active = true;
}

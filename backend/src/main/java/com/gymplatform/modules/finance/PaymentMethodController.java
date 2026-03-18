package com.gymplatform.modules.finance;

import com.gymplatform.modules.finance.dto.CreatePaymentMethodRequest;
import com.gymplatform.modules.finance.dto.PaymentMethodDto;
import com.gymplatform.shared.ApiResponse;
import com.gymplatform.shared.BusinessException;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/members/{memberId}/payment-methods")
@PreAuthorize("hasAnyRole('STUDIO_OWNER', 'MANAGER', 'RECEPTIONIST')")
@RequiredArgsConstructor
public class PaymentMethodController {

    private final PaymentMethodRepository paymentMethodRepository;

    @PostMapping
    public ResponseEntity<ApiResponse<PaymentMethodDto>> addPaymentMethod(
            @PathVariable UUID memberId,
            @Valid @RequestBody CreatePaymentMethodRequest request) {
        PaymentMethod pm = PaymentMethod.builder()
                .memberId(memberId)
                .type(request.getType())
                .stripePaymentMethodId(request.getStripePaymentMethodId())
                .goCardlessMandateId(request.getGoCardlessMandateId())
                .last4(request.getLast4())
                .isDefault(request.isDefault())
                .active(true)
                .build();
        pm = paymentMethodRepository.save(pm);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(toDto(pm)));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<PaymentMethodDto>>> getPaymentMethods(
            @PathVariable UUID memberId) {
        List<PaymentMethodDto> methods = paymentMethodRepository
                .findByMemberIdAndActiveTrue(memberId)
                .stream().map(this::toDto).toList();
        return ResponseEntity.ok(ApiResponse.success(methods));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deletePaymentMethod(@PathVariable UUID id) {
        PaymentMethod pm = paymentMethodRepository.findById(id)
                .orElseThrow(() -> BusinessException.notFound("PaymentMethod", id));
        pm.setActive(false);
        paymentMethodRepository.save(pm);
        return ResponseEntity.ok(ApiResponse.success(null));
    }

    private PaymentMethodDto toDto(PaymentMethod pm) {
        return PaymentMethodDto.builder()
                .id(pm.getId())
                .memberId(pm.getMemberId())
                .type(pm.getType())
                .last4(pm.getLast4())
                .isDefault(pm.isDefault())
                .active(pm.isActive())
                .build();
    }
}

package com.gymplatform.modules.sales;

import com.gymplatform.config.multitenancy.TenantContext;
import com.gymplatform.modules.sales.dto.CreatePromoCodeRequest;
import com.gymplatform.modules.sales.dto.PromoCodeDto;
import com.gymplatform.shared.AuditLogService;
import com.gymplatform.shared.BusinessException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class PromoCodeService {

    private final PromoCodeRepository promoCodeRepository;
    private final PromoCodeUsageRepository usageRepository;
    private final AuditLogService auditLogService;

    @Transactional
    public PromoCodeDto create(CreatePromoCodeRequest req, UUID userId) {
        promoCodeRepository.findByCodeAndActiveTrue(req.getCode()).ifPresent(existing -> {
            throw BusinessException.conflict("Promo code already exists: " + req.getCode());
        });

        PromoCode code = PromoCode.builder()
                .code(req.getCode().toUpperCase())
                .description(req.getDescription())
                .discountType(req.getDiscountType())
                .discountValue(req.getDiscountValue())
                .expiresAt(req.getExpiresAt())
                .maxUsages(req.getMaxUsages())
                .currentUsages(0)
                .active(true)
                .tenantId(TenantContext.getTenantId())
                .build();
        code = promoCodeRepository.save(code);
        auditLogService.log("PromoCode", code.getId(), "CREATE", userId, null, null);
        return toDto(code);
    }

    public PromoCodeDto validate(String codeStr) {
        PromoCode code = promoCodeRepository.findByCodeAndActiveTrue(codeStr.toUpperCase())
                .orElseThrow(() -> BusinessException.notFound("PromoCode", codeStr));

        if (code.getExpiresAt() != null && Instant.now().isAfter(code.getExpiresAt())) {
            throw BusinessException.badRequest("Promo code has expired");
        }
        if (code.getMaxUsages() != null && code.getCurrentUsages() >= code.getMaxUsages()) {
            throw BusinessException.badRequest("Promo code usage limit reached");
        }
        return toDto(code);
    }

    @Transactional
    public void redeem(String codeStr, UUID memberId, UUID contractId) {
        PromoCode code = promoCodeRepository.findByCodeAndActiveTrue(codeStr.toUpperCase())
                .orElseThrow(() -> BusinessException.notFound("PromoCode", codeStr));

        if (code.getExpiresAt() != null && Instant.now().isAfter(code.getExpiresAt())) {
            throw BusinessException.badRequest("Promo code has expired");
        }
        if (code.getMaxUsages() != null && code.getCurrentUsages() >= code.getMaxUsages()) {
            throw BusinessException.badRequest("Promo code usage limit reached");
        }

        code.setCurrentUsages(code.getCurrentUsages() + 1);
        promoCodeRepository.save(code);

        PromoCodeUsage usage = PromoCodeUsage.builder()
                .promoCodeId(code.getId())
                .memberId(memberId)
                .contractId(contractId)
                .usedAt(Instant.now())
                .tenantId(TenantContext.getTenantId())
                .build();
        usageRepository.save(usage);
    }

    @Transactional
    public void deactivate(UUID id, UUID userId) {
        PromoCode code = promoCodeRepository.findById(id)
                .orElseThrow(() -> BusinessException.notFound("PromoCode", id));
        code.setActive(false);
        promoCodeRepository.save(code);
        auditLogService.log("PromoCode", id, "DEACTIVATE", userId, null, null);
    }

    public Page<PromoCodeDto> getAll(Pageable pageable) {
        return promoCodeRepository.findAllByOrderByCreatedAtDesc(pageable).map(this::toDto);
    }

    public PromoCodeDto getById(UUID id) {
        PromoCode code = promoCodeRepository.findById(id)
                .orElseThrow(() -> BusinessException.notFound("PromoCode", id));
        return toDto(code);
    }

    private PromoCodeDto toDto(PromoCode c) {
        boolean expired = c.getExpiresAt() != null && Instant.now().isAfter(c.getExpiresAt());
        boolean exhausted = c.getMaxUsages() != null && c.getCurrentUsages() >= c.getMaxUsages();
        return PromoCodeDto.builder()
                .id(c.getId())
                .code(c.getCode())
                .description(c.getDescription())
                .discountType(c.getDiscountType())
                .discountValue(c.getDiscountValue())
                .expiresAt(c.getExpiresAt())
                .maxUsages(c.getMaxUsages())
                .currentUsages(c.getCurrentUsages())
                .active(c.isActive())
                .expired(expired)
                .exhausted(exhausted)
                .createdAt(c.getCreatedAt())
                .build();
    }
}

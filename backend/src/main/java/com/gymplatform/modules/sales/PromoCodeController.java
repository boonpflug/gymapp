package com.gymplatform.modules.sales;

import com.gymplatform.modules.sales.dto.CreatePromoCodeRequest;
import com.gymplatform.modules.sales.dto.PromoCodeDto;
import com.gymplatform.shared.ApiResponse;
import com.gymplatform.shared.PageMeta;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/sales/promo-codes")
@RequiredArgsConstructor
public class PromoCodeController {

    private final PromoCodeService promoCodeService;

    @GetMapping
    @PreAuthorize("hasAnyRole('STUDIO_OWNER', 'MANAGER')")
    public ResponseEntity<ApiResponse<List<PromoCodeDto>>> getAll(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        Page<PromoCodeDto> result = promoCodeService.getAll(PageRequest.of(page, size));
        return ResponseEntity.ok(ApiResponse.success(result.getContent(), PageMeta.from(result)));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('STUDIO_OWNER', 'MANAGER')")
    public ResponseEntity<ApiResponse<PromoCodeDto>> getById(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.success(promoCodeService.getById(id)));
    }

    @GetMapping("/validate/{code}")
    @PreAuthorize("hasAnyRole('STUDIO_OWNER', 'MANAGER', 'RECEPTIONIST')")
    public ResponseEntity<ApiResponse<PromoCodeDto>> validate(@PathVariable String code) {
        return ResponseEntity.ok(ApiResponse.success(promoCodeService.validate(code)));
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('STUDIO_OWNER', 'MANAGER')")
    public ResponseEntity<ApiResponse<PromoCodeDto>> create(
            @Valid @RequestBody CreatePromoCodeRequest req,
            @AuthenticationPrincipal UserDetails user) {
        UUID userId = UUID.fromString(user.getUsername());
        return ResponseEntity.ok(ApiResponse.success(promoCodeService.create(req, userId)));
    }

    @PostMapping("/{id}/deactivate")
    @PreAuthorize("hasAnyRole('STUDIO_OWNER', 'MANAGER')")
    public ResponseEntity<ApiResponse<Void>> deactivate(
            @PathVariable UUID id,
            @AuthenticationPrincipal UserDetails user) {
        UUID userId = UUID.fromString(user.getUsername());
        promoCodeService.deactivate(id, userId);
        return ResponseEntity.ok(ApiResponse.success(null));
    }
}

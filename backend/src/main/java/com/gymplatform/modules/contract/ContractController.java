package com.gymplatform.modules.contract;

import com.gymplatform.modules.contract.dto.*;
import com.gymplatform.shared.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/contracts")
@PreAuthorize("hasAnyRole('STUDIO_OWNER', 'MANAGER', 'RECEPTIONIST')")
@RequiredArgsConstructor
public class ContractController {

    private final ContractService contractService;

    @PostMapping
    public ResponseEntity<ApiResponse<ContractDto>> createContract(
            @Valid @RequestBody CreateContractRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(contractService.createContract(request)));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<ContractDto>> getContract(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.success(contractService.getContract(id)));
    }

    @GetMapping("/member/{memberId}")
    public ResponseEntity<ApiResponse<List<ContractDto>>> getContractsByMember(
            @PathVariable UUID memberId) {
        return ResponseEntity.ok(ApiResponse.success(
                contractService.getContractsByMember(memberId)));
    }

    @PostMapping("/{id}/freeze")
    public ResponseEntity<ApiResponse<ContractDto>> freezeContract(
            @PathVariable UUID id,
            @Valid @RequestBody FreezeContractRequest request) {
        return ResponseEntity.ok(ApiResponse.success(
                contractService.freezeContract(id, request)));
    }

    @PostMapping("/{id}/unfreeze")
    public ResponseEntity<ApiResponse<ContractDto>> unfreezeContract(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.success(contractService.unfreezeContract(id)));
    }

    @PostMapping("/{id}/cancel")
    public ResponseEntity<ApiResponse<ContractDto>> cancelContract(
            @PathVariable UUID id,
            @RequestBody CancelContractRequest request) {
        return ResponseEntity.ok(ApiResponse.success(
                contractService.requestCancellation(id, request)));
    }

    @PostMapping("/{id}/withdraw-cancellation")
    public ResponseEntity<ApiResponse<ContractDto>> withdrawCancellation(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.success(
                contractService.withdrawCancellation(id)));
    }

    @PutMapping("/{id}/auto-renew")
    public ResponseEntity<ApiResponse<ContractDto>> toggleAutoRenew(
            @PathVariable UUID id,
            @RequestParam boolean enabled) {
        return ResponseEntity.ok(ApiResponse.success(
                contractService.toggleAutoRenew(id, enabled)));
    }

    @PutMapping("/{id}/renewal-settings")
    public ResponseEntity<ApiResponse<ContractDto>> updateRenewalSettings(
            @PathVariable UUID id,
            @RequestBody RenewalSettingsRequest request) {
        return ResponseEntity.ok(ApiResponse.success(
                contractService.updateRenewalSettings(id, request.getRenewalTermMonths(), request.getRenewalNoticeDays())));
    }
}

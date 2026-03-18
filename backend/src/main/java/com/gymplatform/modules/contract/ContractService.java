package com.gymplatform.modules.contract;

import com.gymplatform.config.multitenancy.TenantContext;
import com.gymplatform.modules.contract.dto.*;
import com.gymplatform.shared.AuditLogService;
import com.gymplatform.shared.BusinessException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class ContractService {

    private final ContractRepository contractRepository;
    private final MembershipTierRepository membershipTierRepository;
    private final IdlePeriodRepository idlePeriodRepository;
    private final AuditLogService auditLogService;

    @Transactional
    public ContractDto createContract(CreateContractRequest request) {
        MembershipTier tier = membershipTierRepository.findById(request.getMembershipTierId())
                .orElseThrow(() -> BusinessException.notFound("MembershipTier",
                        request.getMembershipTierId()));

        LocalDate startDate = request.getStartDate() != null ? request.getStartDate() : LocalDate.now();

        Contract contract = Contract.builder()
                .memberId(request.getMemberId())
                .membershipTierId(tier.getId())
                .status(ContractStatus.ACTIVE)
                .startDate(startDate)
                .billingStartDate(startDate)
                .nextBillingDate(startDate.plusMonths(1))
                .monthlyAmount(tier.getMonthlyPrice())
                .discountCode(request.getDiscountCode())
                .autoRenew(true)
                .tenantId(TenantContext.getTenantId())
                .build();

        if (tier.getMinimumTermMonths() > 0) {
            contract.setEndDate(startDate.plusMonths(tier.getMinimumTermMonths()));
        }

        contract = contractRepository.save(contract);
        auditLogService.log("Contract", contract.getId(), "CREATE", null, null, null);
        return toDto(contract, tier.getName());
    }

    @Transactional
    public ContractDto freezeContract(UUID contractId, FreezeContractRequest request) {
        Contract contract = contractRepository.findById(contractId)
                .orElseThrow(() -> BusinessException.notFound("Contract", contractId));

        if (contract.getStatus() != ContractStatus.ACTIVE) {
            throw BusinessException.badRequest("Only active contracts can be frozen");
        }

        if (request.getStartDate().isAfter(request.getEndDate())) {
            throw BusinessException.badRequest("Start date must be before end date");
        }

        contract.setStatus(ContractStatus.PAUSED);

        long freezeDays = request.getEndDate().toEpochDay() - request.getStartDate().toEpochDay();
        contract.setNextBillingDate(contract.getNextBillingDate().plusDays(freezeDays));
        if (contract.getEndDate() != null) {
            contract.setEndDate(contract.getEndDate().plusDays(freezeDays));
        }

        IdlePeriod idlePeriod = IdlePeriod.builder()
                .contractId(contractId)
                .startDate(request.getStartDate())
                .endDate(request.getEndDate())
                .reason(request.getReason())
                .build();

        idlePeriodRepository.save(idlePeriod);
        contract = contractRepository.save(contract);
        auditLogService.log("Contract", contractId, "FREEZE", null, null, null);

        String tierName = membershipTierRepository.findById(contract.getMembershipTierId())
                .map(MembershipTier::getName).orElse("");
        return toDto(contract, tierName);
    }

    @Transactional
    public ContractDto unfreezeContract(UUID contractId) {
        Contract contract = contractRepository.findById(contractId)
                .orElseThrow(() -> BusinessException.notFound("Contract", contractId));

        if (contract.getStatus() != ContractStatus.PAUSED) {
            throw BusinessException.badRequest("Contract is not frozen");
        }

        contract.setStatus(ContractStatus.ACTIVE);
        contract = contractRepository.save(contract);
        auditLogService.log("Contract", contractId, "UNFREEZE", null, null, null);

        String tierName = membershipTierRepository.findById(contract.getMembershipTierId())
                .map(MembershipTier::getName).orElse("");
        return toDto(contract, tierName);
    }

    @Transactional
    public ContractDto requestCancellation(UUID contractId, CancelContractRequest request) {
        Contract contract = contractRepository.findById(contractId)
                .orElseThrow(() -> BusinessException.notFound("Contract", contractId));

        if (contract.getStatus() == ContractStatus.CANCELLED) {
            throw BusinessException.badRequest("Contract is already cancelled");
        }

        UUID tierId = contract.getMembershipTierId();
        MembershipTier tier = membershipTierRepository.findById(tierId)
                .orElseThrow(() -> BusinessException.notFound("MembershipTier", tierId));

        LocalDate effectiveDate = LocalDate.now().plusDays(tier.getNoticePeriodDays());
        if (contract.getEndDate() != null && effectiveDate.isBefore(contract.getEndDate())) {
            effectiveDate = contract.getEndDate();
        }

        contract.setStatus(ContractStatus.PENDING_CANCELLATION);
        contract.setCancellationDate(LocalDate.now());
        contract.setCancellationEffectiveDate(effectiveDate);
        contract.setCancellationReason(request.getReason());

        contract = contractRepository.save(contract);
        auditLogService.log("Contract", contractId, "CANCEL_REQUEST", null, null, null);
        return toDto(contract, tier.getName());
    }

    @Transactional
    public ContractDto withdrawCancellation(UUID contractId) {
        Contract contract = contractRepository.findById(contractId)
                .orElseThrow(() -> BusinessException.notFound("Contract", contractId));

        if (contract.getStatus() != ContractStatus.PENDING_CANCELLATION) {
            throw BusinessException.badRequest("Contract is not pending cancellation");
        }

        contract.setStatus(ContractStatus.ACTIVE);
        contract.setCancellationDate(null);
        contract.setCancellationEffectiveDate(null);
        contract.setCancellationReason(null);

        contract = contractRepository.save(contract);
        auditLogService.log("Contract", contractId, "CANCEL_WITHDRAW", null, null, null);

        String tierName = membershipTierRepository.findById(contract.getMembershipTierId())
                .map(MembershipTier::getName).orElse("");
        return toDto(contract, tierName);
    }

    public List<ContractDto> getContractsByMember(UUID memberId) {
        return contractRepository.findByMemberId(memberId).stream()
                .map(c -> {
                    String tierName = membershipTierRepository.findById(c.getMembershipTierId())
                            .map(MembershipTier::getName).orElse("");
                    return toDto(c, tierName);
                }).toList();
    }

    public ContractDto getContract(UUID id) {
        Contract contract = contractRepository.findById(id)
                .orElseThrow(() -> BusinessException.notFound("Contract", id));
        String tierName = membershipTierRepository.findById(contract.getMembershipTierId())
                .map(MembershipTier::getName).orElse("");
        return toDto(contract, tierName);
    }

    private ContractDto toDto(Contract c, String tierName) {
        return ContractDto.builder()
                .id(c.getId())
                .memberId(c.getMemberId())
                .membershipTierId(c.getMembershipTierId())
                .membershipTierName(tierName)
                .status(c.getStatus())
                .startDate(c.getStartDate())
                .endDate(c.getEndDate())
                .nextBillingDate(c.getNextBillingDate())
                .monthlyAmount(c.getMonthlyAmount())
                .discountCode(c.getDiscountCode())
                .cancellationDate(c.getCancellationDate())
                .cancellationEffectiveDate(c.getCancellationEffectiveDate())
                .cancellationReason(c.getCancellationReason())
                .autoRenew(c.isAutoRenew())
                .build();
    }
}

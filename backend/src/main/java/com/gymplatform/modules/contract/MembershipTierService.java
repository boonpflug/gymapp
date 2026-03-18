package com.gymplatform.modules.contract;

import com.gymplatform.config.multitenancy.TenantContext;
import com.gymplatform.modules.contract.dto.CreateMembershipTierRequest;
import com.gymplatform.modules.contract.dto.MembershipTierDto;
import com.gymplatform.shared.BusinessException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class MembershipTierService {

    private final MembershipTierRepository membershipTierRepository;

    @Transactional
    public MembershipTierDto createTier(CreateMembershipTierRequest request) {
        MembershipTier tier = MembershipTier.builder()
                .name(request.getName())
                .description(request.getDescription())
                .monthlyPrice(request.getMonthlyPrice())
                .billingCycle(request.getBillingCycle())
                .minimumTermMonths(request.getMinimumTermMonths())
                .noticePeriodDays(request.getNoticePeriodDays())
                .classAllowance(request.getClassAllowance())
                .accessRules(request.getAccessRules())
                .active(true)
                .tenantId(TenantContext.getTenantId())
                .build();
        tier = membershipTierRepository.save(tier);
        return toDto(tier);
    }

    public List<MembershipTierDto> getActiveTiers() {
        return membershipTierRepository.findByActiveTrue().stream()
                .map(this::toDto).toList();
    }

    public MembershipTierDto getTier(UUID id) {
        return toDto(membershipTierRepository.findById(id)
                .orElseThrow(() -> BusinessException.notFound("MembershipTier", id)));
    }

    @Transactional
    public MembershipTierDto updateTier(UUID id, CreateMembershipTierRequest request) {
        MembershipTier tier = membershipTierRepository.findById(id)
                .orElseThrow(() -> BusinessException.notFound("MembershipTier", id));
        tier.setName(request.getName());
        tier.setDescription(request.getDescription());
        tier.setMonthlyPrice(request.getMonthlyPrice());
        tier.setBillingCycle(request.getBillingCycle());
        tier.setMinimumTermMonths(request.getMinimumTermMonths());
        tier.setNoticePeriodDays(request.getNoticePeriodDays());
        tier.setClassAllowance(request.getClassAllowance());
        tier.setAccessRules(request.getAccessRules());
        tier = membershipTierRepository.save(tier);
        return toDto(tier);
    }

    @Transactional
    public void deleteTier(UUID id) {
        MembershipTier tier = membershipTierRepository.findById(id)
                .orElseThrow(() -> BusinessException.notFound("MembershipTier", id));
        tier.setActive(false);
        membershipTierRepository.save(tier);
    }

    private MembershipTierDto toDto(MembershipTier t) {
        return MembershipTierDto.builder()
                .id(t.getId())
                .name(t.getName())
                .description(t.getDescription())
                .monthlyPrice(t.getMonthlyPrice())
                .billingCycle(t.getBillingCycle())
                .minimumTermMonths(t.getMinimumTermMonths())
                .noticePeriodDays(t.getNoticePeriodDays())
                .classAllowance(t.getClassAllowance())
                .active(t.isActive())
                .build();
    }
}

package com.gymplatform.modules.checkin;

import com.gymplatform.config.multitenancy.TenantContext;
import com.gymplatform.modules.checkin.dto.AccessRestrictionDto;
import com.gymplatform.modules.checkin.dto.CreateRestrictionRequest;
import com.gymplatform.shared.AuditLogService;
import com.gymplatform.shared.BusinessException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AccessRestrictionService {

    private final AccessRestrictionRepository restrictionRepository;
    private final AuditLogService auditLogService;

    public List<AccessRestrictionDto> getActiveRestrictions(UUID memberId) {
        return restrictionRepository.findByMemberIdAndActiveTrue(memberId).stream()
                .map(this::toDto)
                .toList();
    }

    public List<AccessRestrictionDto> getAllRestrictions(UUID memberId) {
        return restrictionRepository.findByMemberId(memberId).stream()
                .map(this::toDto)
                .toList();
    }

    public boolean hasActiveRestriction(UUID memberId) {
        return !restrictionRepository.findByMemberIdAndActiveTrue(memberId).isEmpty();
    }

    public String getActiveRestrictionReason(UUID memberId) {
        List<AccessRestriction> restrictions = restrictionRepository.findByMemberIdAndActiveTrue(memberId);
        if (restrictions.isEmpty()) return null;
        AccessRestriction first = restrictions.get(0);
        return first.getReason().name() + ": " +
                (first.getDescription() != null ? first.getDescription() : first.getReason().name());
    }

    @Transactional
    public AccessRestrictionDto createRestriction(CreateRestrictionRequest request, UUID staffId) {
        // Check if same type of restriction already active
        List<AccessRestriction> existing = restrictionRepository
                .findByMemberIdAndReasonAndActiveTrue(request.getMemberId(), request.getReason());
        if (!existing.isEmpty()) {
            throw BusinessException.conflict("Active restriction of type " + request.getReason() + " already exists");
        }

        AccessRestriction restriction = AccessRestriction.builder()
                .memberId(request.getMemberId())
                .reason(request.getReason())
                .description(request.getDescription())
                .active(true)
                .restrictedBy(staffId)
                .restrictedAt(Instant.now())
                .autoRelease(request.isAutoRelease())
                .tenantId(TenantContext.getTenantId())
                .build();

        restriction = restrictionRepository.save(restriction);

        auditLogService.log("AccessRestriction", restriction.getId(), "CREATE",
                staffId, null, restriction.toString());

        return toDto(restriction);
    }

    @Transactional
    public AccessRestrictionDto releaseRestriction(UUID restrictionId, UUID staffId) {
        AccessRestriction restriction = restrictionRepository.findById(restrictionId)
                .orElseThrow(() -> BusinessException.notFound("Restriction", restrictionId));

        if (!restriction.isActive()) {
            throw BusinessException.badRequest("Restriction is already released");
        }

        restriction.setActive(false);
        restriction.setReleasedAt(Instant.now());
        restriction.setReleasedBy(staffId);
        restriction = restrictionRepository.save(restriction);

        auditLogService.log("AccessRestriction", restriction.getId(), "RELEASE",
                staffId, "active=true", "active=false");

        return toDto(restriction);
    }

    @Transactional
    public void releaseRestrictionsByReason(UUID memberId, RestrictionReason reason) {
        List<AccessRestriction> restrictions = restrictionRepository
                .findByMemberIdAndReasonAndActiveTrue(memberId, reason);
        for (AccessRestriction r : restrictions) {
            r.setActive(false);
            r.setReleasedAt(Instant.now());
            r.setAutoRelease(true);
            restrictionRepository.save(r);
        }
    }

    private AccessRestrictionDto toDto(AccessRestriction r) {
        return AccessRestrictionDto.builder()
                .id(r.getId())
                .memberId(r.getMemberId())
                .reason(r.getReason())
                .description(r.getDescription())
                .active(r.isActive())
                .restrictedBy(r.getRestrictedBy())
                .restrictedAt(r.getRestrictedAt())
                .releasedAt(r.getReleasedAt())
                .releasedBy(r.getReleasedBy())
                .autoRelease(r.isAutoRelease())
                .build();
    }
}

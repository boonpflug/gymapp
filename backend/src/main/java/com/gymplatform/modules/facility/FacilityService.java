package com.gymplatform.modules.facility;

import com.gymplatform.config.multitenancy.TenantContext;
import com.gymplatform.modules.facility.dto.*;
import com.gymplatform.shared.AuditLogService;
import com.gymplatform.shared.BusinessException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class FacilityService {

    private final FacilityRepository facilityRepository;
    private final FacilityConfigurationRepository configRepository;
    private final MemberFacilityAccessRepository memberAccessRepository;
    private final AuditLogService auditLogService;

    public Page<FacilityDto> getAll(Pageable pageable) {
        return facilityRepository.findByActiveTrueOrderByNameAsc(pageable).map(this::toDto);
    }

    public List<FacilityDto> getAllList() {
        return facilityRepository.findByActiveTrueOrderByNameAsc().stream()
                .map(this::toDto).collect(Collectors.toList());
    }

    public FacilityDto getById(UUID id) {
        Facility facility = facilityRepository.findById(id)
                .orElseThrow(() -> BusinessException.notFound("Facility", id));
        FacilityDto dto = toDto(facility);
        List<FacilityDto> children = facilityRepository.findByParentFacilityId(id).stream()
                .map(this::toDto).collect(Collectors.toList());
        dto.setChildFacilities(children);
        return dto;
    }

    @Transactional
    public FacilityDto create(CreateFacilityRequest req, UUID userId) {
        if (req.getParentFacilityId() != null) {
            facilityRepository.findById(req.getParentFacilityId())
                    .orElseThrow(() -> BusinessException.notFound("Parent Facility", req.getParentFacilityId()));
        }

        Facility facility = Facility.builder()
                .name(req.getName())
                .description(req.getDescription())
                .street(req.getStreet())
                .city(req.getCity())
                .state(req.getState())
                .postalCode(req.getPostalCode())
                .country(req.getCountry())
                .timezone(req.getTimezone())
                .phone(req.getPhone())
                .email(req.getEmail())
                .websiteUrl(req.getWebsiteUrl())
                .openingHours(req.getOpeningHours())
                .logoUrl(req.getLogoUrl())
                .brandColor(req.getBrandColor())
                .bannerImageUrl(req.getBannerImageUrl())
                .maxOccupancy(req.getMaxOccupancy())
                .parentFacilityId(req.getParentFacilityId())
                .active(true)
                .tenantId(TenantContext.getTenantId())
                .build();
        facility = facilityRepository.save(facility);
        auditLogService.log("Facility", facility.getId(), "CREATE", userId, null, null);
        return toDto(facility);
    }

    @Transactional
    public FacilityDto update(UUID id, CreateFacilityRequest req, UUID userId) {
        Facility facility = facilityRepository.findById(id)
                .orElseThrow(() -> BusinessException.notFound("Facility", id));
        facility.setName(req.getName());
        facility.setDescription(req.getDescription());
        facility.setStreet(req.getStreet());
        facility.setCity(req.getCity());
        facility.setState(req.getState());
        facility.setPostalCode(req.getPostalCode());
        facility.setCountry(req.getCountry());
        facility.setTimezone(req.getTimezone());
        facility.setPhone(req.getPhone());
        facility.setEmail(req.getEmail());
        facility.setWebsiteUrl(req.getWebsiteUrl());
        facility.setOpeningHours(req.getOpeningHours());
        facility.setLogoUrl(req.getLogoUrl());
        facility.setBrandColor(req.getBrandColor());
        facility.setBannerImageUrl(req.getBannerImageUrl());
        facility.setMaxOccupancy(req.getMaxOccupancy());
        facility.setParentFacilityId(req.getParentFacilityId());
        facility = facilityRepository.save(facility);
        auditLogService.log("Facility", facility.getId(), "UPDATE", userId, null, null);
        return toDto(facility);
    }

    @Transactional
    public void deactivate(UUID id, UUID userId) {
        Facility facility = facilityRepository.findById(id)
                .orElseThrow(() -> BusinessException.notFound("Facility", id));
        facility.setActive(false);
        facilityRepository.save(facility);
        auditLogService.log("Facility", id, "DEACTIVATE", userId, null, null);
    }

    // --- Configuration ---

    public List<FacilityConfigDto> getConfigurations(UUID facilityId) {
        return configRepository.findByFacilityId(facilityId).stream()
                .map(this::toConfigDto).collect(Collectors.toList());
    }

    @Transactional
    public FacilityConfigDto setConfiguration(UUID facilityId, SetFacilityConfigRequest req, UUID userId) {
        facilityRepository.findById(facilityId)
                .orElseThrow(() -> BusinessException.notFound("Facility", facilityId));

        FacilityConfiguration config = configRepository
                .findByFacilityIdAndConfigKey(facilityId, req.getConfigKey())
                .orElse(FacilityConfiguration.builder()
                        .facilityId(facilityId)
                        .configKey(req.getConfigKey())
                        .tenantId(TenantContext.getTenantId())
                        .build());
        config.setConfigValue(req.getConfigValue());
        config.setDescription(req.getDescription());
        config = configRepository.save(config);
        auditLogService.log("FacilityConfiguration", config.getId(), "SET", userId, null, null);
        return toConfigDto(config);
    }

    @Transactional
    public void deleteConfiguration(UUID facilityId, String configKey, UUID userId) {
        configRepository.deleteByFacilityIdAndConfigKey(facilityId, configKey);
        auditLogService.log("FacilityConfiguration", facilityId, "DELETE_CONFIG", userId, null, configKey);
    }

    // --- Member facility access ---

    public List<MemberFacilityAccessDto> getMemberAccess(UUID memberId) {
        return memberAccessRepository.findByMemberId(memberId).stream()
                .map(this::toAccessDto).collect(Collectors.toList());
    }

    public List<MemberFacilityAccessDto> getFacilityMembers(UUID facilityId) {
        return memberAccessRepository.findByFacilityId(facilityId).stream()
                .map(this::toAccessDto).collect(Collectors.toList());
    }

    @Transactional
    public MemberFacilityAccessDto assignMemberToFacility(AssignMemberFacilityRequest req, UUID userId) {
        facilityRepository.findById(req.getFacilityId())
                .orElseThrow(() -> BusinessException.notFound("Facility", req.getFacilityId()));

        var existing = memberAccessRepository.findByMemberIdAndFacilityId(req.getMemberId(), req.getFacilityId());
        if (existing.isPresent()) {
            throw BusinessException.conflict("Member is already assigned to this facility");
        }

        if (req.isHomeFacility()) {
            memberAccessRepository.findByMemberIdAndHomeFacilityTrue(req.getMemberId())
                    .ifPresent(current -> {
                        current.setHomeFacility(false);
                        memberAccessRepository.save(current);
                    });
        }

        MemberFacilityAccess access = MemberFacilityAccess.builder()
                .memberId(req.getMemberId())
                .facilityId(req.getFacilityId())
                .homeFacility(req.isHomeFacility())
                .crossFacilityAccess(req.isCrossFacilityAccess())
                .tenantId(TenantContext.getTenantId())
                .build();
        access = memberAccessRepository.save(access);
        auditLogService.log("MemberFacilityAccess", access.getId(), "ASSIGN", userId, null, null);
        return toAccessDto(access);
    }

    @Transactional
    public void removeMemberFromFacility(UUID memberId, UUID facilityId, UUID userId) {
        var access = memberAccessRepository.findByMemberIdAndFacilityId(memberId, facilityId)
                .orElseThrow(() -> BusinessException.badRequest("Member is not assigned to this facility"));
        memberAccessRepository.delete(access);
        auditLogService.log("MemberFacilityAccess", access.getId(), "REMOVE", userId, null, null);
    }

    public boolean canMemberAccessFacility(UUID memberId, UUID facilityId) {
        var access = memberAccessRepository.findByMemberIdAndFacilityId(memberId, facilityId);
        if (access.isPresent()) {
            return access.get().isCrossFacilityAccess() || access.get().isHomeFacility();
        }
        return false;
    }

    // --- DTO mappers ---

    FacilityDto toDto(Facility f) {
        String parentName = null;
        if (f.getParentFacilityId() != null) {
            parentName = facilityRepository.findById(f.getParentFacilityId())
                    .map(Facility::getName).orElse(null);
        }
        long memberCount = memberAccessRepository.countByFacilityId(f.getId());

        return FacilityDto.builder()
                .id(f.getId())
                .name(f.getName())
                .description(f.getDescription())
                .street(f.getStreet())
                .city(f.getCity())
                .state(f.getState())
                .postalCode(f.getPostalCode())
                .country(f.getCountry())
                .timezone(f.getTimezone())
                .phone(f.getPhone())
                .email(f.getEmail())
                .websiteUrl(f.getWebsiteUrl())
                .openingHours(f.getOpeningHours())
                .logoUrl(f.getLogoUrl())
                .brandColor(f.getBrandColor())
                .bannerImageUrl(f.getBannerImageUrl())
                .maxOccupancy(f.getMaxOccupancy())
                .parentFacilityId(f.getParentFacilityId())
                .parentFacilityName(parentName)
                .active(f.isActive())
                .memberCount(memberCount)
                .createdAt(f.getCreatedAt())
                .build();
    }

    private FacilityConfigDto toConfigDto(FacilityConfiguration c) {
        return FacilityConfigDto.builder()
                .id(c.getId())
                .facilityId(c.getFacilityId())
                .configKey(c.getConfigKey())
                .configValue(c.getConfigValue())
                .description(c.getDescription())
                .build();
    }

    private MemberFacilityAccessDto toAccessDto(MemberFacilityAccess a) {
        String facilityName = facilityRepository.findById(a.getFacilityId())
                .map(Facility::getName).orElse(null);
        return MemberFacilityAccessDto.builder()
                .id(a.getId())
                .memberId(a.getMemberId())
                .facilityId(a.getFacilityId())
                .facilityName(facilityName)
                .homeFacility(a.isHomeFacility())
                .crossFacilityAccess(a.isCrossFacilityAccess())
                .build();
    }
}

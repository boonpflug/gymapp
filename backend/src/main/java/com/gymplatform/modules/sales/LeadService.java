package com.gymplatform.modules.sales;

import com.gymplatform.config.multitenancy.TenantContext;
import com.gymplatform.modules.member.Member;
import com.gymplatform.modules.member.MemberRepository;
import com.gymplatform.modules.member.MemberStatus;
import com.gymplatform.modules.sales.dto.*;
import com.gymplatform.shared.AuditLogService;
import com.gymplatform.shared.BusinessException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class LeadService {

    private final LeadRepository leadRepository;
    private final LeadStageRepository stageRepository;
    private final LeadActivityRepository activityRepository;
    private final MemberRepository memberRepository;
    private final AuditLogService auditLogService;
    private final RabbitTemplate rabbitTemplate;

    @Transactional
    public LeadDto create(CreateLeadRequest req, UUID userId) {
        LeadStage defaultStage = stageRepository.findByIsDefaultTrue()
                .orElseThrow(() -> BusinessException.badRequest("No default lead stage configured"));

        Lead lead = Lead.builder()
                .firstName(req.getFirstName())
                .lastName(req.getLastName())
                .email(req.getEmail())
                .phone(req.getPhone())
                .source(req.getSource())
                .interest(req.getInterest())
                .stageId(defaultStage.getId())
                .assignedStaffId(req.getAssignedStaffId())
                .notes(req.getNotes())
                .referralMemberId(req.getReferralMemberId())
                .tenantId(TenantContext.getTenantId())
                .build();
        lead = leadRepository.save(lead);

        // Log initial activity
        LeadActivity activity = LeadActivity.builder()
                .leadId(lead.getId())
                .activityType(LeadActivityType.NOTE)
                .description("Lead created from " + req.getSource())
                .staffId(userId)
                .tenantId(TenantContext.getTenantId())
                .build();
        activityRepository.save(activity);

        auditLogService.log("Lead", lead.getId(), "CREATE", userId, null, null);
        return toDto(lead);
    }

    @Transactional
    public LeadDto update(UUID id, CreateLeadRequest req, UUID userId) {
        Lead lead = leadRepository.findById(id)
                .orElseThrow(() -> BusinessException.notFound("Lead", id));
        lead.setFirstName(req.getFirstName());
        lead.setLastName(req.getLastName());
        lead.setEmail(req.getEmail());
        lead.setPhone(req.getPhone());
        lead.setSource(req.getSource());
        lead.setInterest(req.getInterest());
        lead.setAssignedStaffId(req.getAssignedStaffId());
        lead.setNotes(req.getNotes());
        lead = leadRepository.save(lead);
        auditLogService.log("Lead", lead.getId(), "UPDATE", userId, null, null);
        return toDto(lead);
    }

    @Transactional
    public LeadDto moveToStage(UUID leadId, UUID stageId, UUID userId) {
        Lead lead = leadRepository.findById(leadId)
                .orElseThrow(() -> BusinessException.notFound("Lead", leadId));
        LeadStage stage = stageRepository.findById(stageId)
                .orElseThrow(() -> BusinessException.notFound("LeadStage", stageId));

        UUID oldStageId = lead.getStageId();
        lead.setStageId(stageId);
        lead = leadRepository.save(lead);

        LeadActivity activity = LeadActivity.builder()
                .leadId(leadId)
                .activityType(LeadActivityType.NOTE)
                .description("Moved to stage: " + stage.getName())
                .staffId(userId)
                .tenantId(TenantContext.getTenantId())
                .build();
        activityRepository.save(activity);

        auditLogService.log("Lead", leadId, "MOVE_STAGE", userId,
                oldStageId.toString(), stageId.toString());
        return toDto(lead);
    }

    @Transactional
    public LeadDto assignStaff(UUID leadId, UUID staffId, UUID userId) {
        Lead lead = leadRepository.findById(leadId)
                .orElseThrow(() -> BusinessException.notFound("Lead", leadId));
        lead.setAssignedStaffId(staffId);
        lead = leadRepository.save(lead);
        auditLogService.log("Lead", leadId, "ASSIGN", userId, null, staffId.toString());
        return toDto(lead);
    }

    @Transactional
    public LeadDto convertToMember(UUID leadId, ConvertLeadRequest req, UUID userId) {
        Lead lead = leadRepository.findById(leadId)
                .orElseThrow(() -> BusinessException.notFound("Lead", leadId));

        if (lead.getConvertedMemberId() != null) {
            throw BusinessException.conflict("Lead is already converted");
        }

        // Create member from lead data
        Member member = Member.builder()
                .firstName(lead.getFirstName())
                .lastName(lead.getLastName())
                .email(lead.getEmail())
                .phone(lead.getPhone())
                .status(MemberStatus.ACTIVE)
                .joinDate(LocalDate.now())
                .tenantId(TenantContext.getTenantId())
                .build();
        member = memberRepository.save(member);

        lead.setConvertedMemberId(member.getId());

        // Move to won stage
        Lead finalLead = lead;
        stageRepository.findAllByOrderBySortOrderAsc().stream()
                .filter(LeadStage::isWon)
                .findFirst()
                .ifPresent(wonStage -> finalLead.setStageId(wonStage.getId()));

        lead = leadRepository.save(lead);

        LeadActivity activity = LeadActivity.builder()
                .leadId(leadId)
                .activityType(LeadActivityType.CONTRACT_SIGNED)
                .description("Converted to member: " + member.getId())
                .staffId(userId)
                .tenantId(TenantContext.getTenantId())
                .build();
        activityRepository.save(activity);

        auditLogService.log("Lead", leadId, "CONVERT", userId, null, member.getId().toString());

        try {
            rabbitTemplate.convertAndSend("member.events", "member.created", Map.of(
                    "memberId", member.getId().toString(),
                    "leadId", leadId.toString(),
                    "tenantId", TenantContext.getTenantId(),
                    "timestamp", Instant.now().toString()
            ));
        } catch (Exception e) {
            log.warn("Failed to publish member.created event: {}", e.getMessage());
        }

        return toDto(lead);
    }

    public LeadDto getById(UUID id) {
        Lead lead = leadRepository.findById(id)
                .orElseThrow(() -> BusinessException.notFound("Lead", id));
        return toDto(lead);
    }

    public Page<LeadDto> search(String name, LeadSource source, UUID stageId,
                                 UUID assignedStaffId, Pageable pageable) {
        Specification<Lead> spec = Specification
                .where(LeadSpecification.hasName(name))
                .and(LeadSpecification.hasSource(source))
                .and(LeadSpecification.hasStage(stageId))
                .and(LeadSpecification.hasAssignedStaff(assignedStaffId));
        return leadRepository.findAll(spec, pageable).map(this::toDto);
    }

    public SalesPipelineDto getPipeline() {
        var stages = stageRepository.findAllByOrderBySortOrderAsc().stream()
                .map(s -> LeadStageDto.builder()
                        .id(s.getId()).name(s.getName()).sortOrder(s.getSortOrder())
                        .color(s.getColor()).isDefault(s.isDefault())
                        .isClosed(s.isClosed()).isWon(s.isWon())
                        .leadCount(leadRepository.countByStageId(s.getId()))
                        .build())
                .toList();
        long total = leadRepository.count();
        long converted = leadRepository.countConverted();
        return SalesPipelineDto.builder()
                .stages(stages)
                .totalLeads(total)
                .convertedLeads(converted)
                .conversionRate(total > 0 ? (double) converted / total * 100 : 0)
                .build();
    }

    private LeadDto toDto(Lead l) {
        LeadStage stage = stageRepository.findById(l.getStageId()).orElse(null);
        String staffName = l.getAssignedStaffId() != null
                ? memberRepository.findById(l.getAssignedStaffId())
                    .map(m -> m.getFirstName() + " " + m.getLastName()).orElse(null)
                : null;
        int activityCount = activityRepository.findByLeadIdOrderByCreatedAtDesc(l.getId()).size();

        return LeadDto.builder()
                .id(l.getId())
                .firstName(l.getFirstName())
                .lastName(l.getLastName())
                .email(l.getEmail())
                .phone(l.getPhone())
                .source(l.getSource())
                .interest(l.getInterest())
                .stageId(l.getStageId())
                .stageName(stage != null ? stage.getName() : null)
                .stageColor(stage != null ? stage.getColor() : null)
                .assignedStaffId(l.getAssignedStaffId())
                .assignedStaffName(staffName)
                .notes(l.getNotes())
                .convertedMemberId(l.getConvertedMemberId())
                .referralMemberId(l.getReferralMemberId())
                .activityCount(activityCount)
                .createdAt(l.getCreatedAt())
                .updatedAt(l.getUpdatedAt())
                .build();
    }
}

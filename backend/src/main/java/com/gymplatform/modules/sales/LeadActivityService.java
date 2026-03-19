package com.gymplatform.modules.sales;

import com.gymplatform.config.multitenancy.TenantContext;
import com.gymplatform.modules.member.MemberRepository;
import com.gymplatform.modules.sales.dto.CreateLeadActivityRequest;
import com.gymplatform.modules.sales.dto.LeadActivityDto;
import com.gymplatform.shared.AuditLogService;
import com.gymplatform.shared.BusinessException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class LeadActivityService {

    private final LeadActivityRepository activityRepository;
    private final LeadRepository leadRepository;
    private final MemberRepository memberRepository;
    private final AuditLogService auditLogService;

    @Transactional
    public LeadActivityDto create(CreateLeadActivityRequest req, UUID userId) {
        leadRepository.findById(req.getLeadId())
                .orElseThrow(() -> BusinessException.notFound("Lead", req.getLeadId()));

        LeadActivity activity = LeadActivity.builder()
                .leadId(req.getLeadId())
                .activityType(req.getActivityType())
                .description(req.getDescription())
                .outcome(req.getOutcome())
                .staffId(userId)
                .dueDate(req.getDueDate())
                .tenantId(TenantContext.getTenantId())
                .build();
        activity = activityRepository.save(activity);
        auditLogService.log("LeadActivity", activity.getId(), "CREATE", userId, null, null);
        return toDto(activity);
    }

    @Transactional
    public LeadActivityDto completeTask(UUID activityId, String outcome, UUID userId) {
        LeadActivity activity = activityRepository.findById(activityId)
                .orElseThrow(() -> BusinessException.notFound("LeadActivity", activityId));
        if (activity.getCompletedAt() != null) {
            throw BusinessException.badRequest("Task is already completed");
        }
        activity.setCompletedAt(Instant.now());
        if (outcome != null) activity.setOutcome(outcome);
        activity = activityRepository.save(activity);
        auditLogService.log("LeadActivity", activityId, "COMPLETE", userId, null, null);
        return toDto(activity);
    }

    public List<LeadActivityDto> getByLead(UUID leadId) {
        return activityRepository.findByLeadIdOrderByCreatedAtDesc(leadId)
                .stream().map(this::toDto).collect(Collectors.toList());
    }

    public List<LeadActivityDto> getOverdueTasks() {
        return activityRepository.findOverdueTasks(LocalDate.now())
                .stream().map(this::toDto).collect(Collectors.toList());
    }

    public List<LeadActivityDto> getPendingTasksByStaff(UUID staffId) {
        return activityRepository.findPendingTasksByStaff(staffId)
                .stream().map(this::toDto).collect(Collectors.toList());
    }

    private LeadActivityDto toDto(LeadActivity a) {
        String staffName = a.getStaffId() != null
                ? memberRepository.findById(a.getStaffId())
                    .map(m -> m.getFirstName() + " " + m.getLastName()).orElse(null)
                : null;
        return LeadActivityDto.builder()
                .id(a.getId())
                .leadId(a.getLeadId())
                .activityType(a.getActivityType())
                .description(a.getDescription())
                .outcome(a.getOutcome())
                .staffId(a.getStaffId())
                .staffName(staffName)
                .dueDate(a.getDueDate())
                .completedAt(a.getCompletedAt())
                .createdAt(a.getCreatedAt())
                .build();
    }
}

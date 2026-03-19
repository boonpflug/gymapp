package com.gymplatform.modules.communication;

import com.gymplatform.config.multitenancy.TenantContext;
import com.gymplatform.modules.communication.dto.CreateNotificationRuleRequest;
import com.gymplatform.modules.communication.dto.NotificationRuleDto;
import com.gymplatform.shared.AuditLogService;
import com.gymplatform.shared.BusinessException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class NotificationRuleService {

    private final NotificationRuleRepository ruleRepository;
    private final CommunicationTemplateRepository templateRepository;
    private final AuditLogService auditLogService;

    public List<NotificationRuleDto> getAll() {
        return ruleRepository.findByActiveTrueOrderByNameAsc()
                .stream().map(this::toDto).collect(Collectors.toList());
    }

    public List<NotificationRuleDto> getByTrigger(TriggerEvent triggerEvent) {
        return ruleRepository.findByTriggerEventAndActiveTrue(triggerEvent)
                .stream().map(this::toDto).collect(Collectors.toList());
    }

    public NotificationRuleDto getById(UUID id) {
        NotificationRule rule = ruleRepository.findById(id)
                .orElseThrow(() -> BusinessException.notFound("NotificationRule", id));
        return toDto(rule);
    }

    @Transactional
    public NotificationRuleDto create(CreateNotificationRuleRequest req, UUID userId) {
        templateRepository.findById(req.getTemplateId())
                .orElseThrow(() -> BusinessException.notFound("CommunicationTemplate", req.getTemplateId()));

        NotificationRule rule = NotificationRule.builder()
                .name(req.getName())
                .triggerEvent(req.getTriggerEvent())
                .templateId(req.getTemplateId())
                .channelType(req.getChannelType())
                .delayDays(req.getDelayDays())
                .delayDirection(req.getDelayDirection())
                .description(req.getDescription())
                .active(true)
                .tenantId(TenantContext.getTenantId())
                .build();
        rule = ruleRepository.save(rule);
        auditLogService.log("NotificationRule", rule.getId(), "CREATE", userId, null, null);
        return toDto(rule);
    }

    @Transactional
    public NotificationRuleDto update(UUID id, CreateNotificationRuleRequest req, UUID userId) {
        NotificationRule rule = ruleRepository.findById(id)
                .orElseThrow(() -> BusinessException.notFound("NotificationRule", id));

        templateRepository.findById(req.getTemplateId())
                .orElseThrow(() -> BusinessException.notFound("CommunicationTemplate", req.getTemplateId()));

        rule.setName(req.getName());
        rule.setTriggerEvent(req.getTriggerEvent());
        rule.setTemplateId(req.getTemplateId());
        rule.setChannelType(req.getChannelType());
        rule.setDelayDays(req.getDelayDays());
        rule.setDelayDirection(req.getDelayDirection());
        rule.setDescription(req.getDescription());
        rule = ruleRepository.save(rule);
        auditLogService.log("NotificationRule", rule.getId(), "UPDATE", userId, null, null);
        return toDto(rule);
    }

    @Transactional
    public void toggleActive(UUID id, UUID userId) {
        NotificationRule rule = ruleRepository.findById(id)
                .orElseThrow(() -> BusinessException.notFound("NotificationRule", id));
        rule.setActive(!rule.isActive());
        ruleRepository.save(rule);
        auditLogService.log("NotificationRule", id, rule.isActive() ? "ACTIVATE" : "DEACTIVATE",
                userId, null, null);
    }

    @Transactional
    public void delete(UUID id, UUID userId) {
        NotificationRule rule = ruleRepository.findById(id)
                .orElseThrow(() -> BusinessException.notFound("NotificationRule", id));
        ruleRepository.delete(rule);
        auditLogService.log("NotificationRule", id, "DELETE", userId, null, null);
    }

    private NotificationRuleDto toDto(NotificationRule r) {
        String templateName = templateRepository.findById(r.getTemplateId())
                .map(CommunicationTemplate::getName).orElse(null);

        return NotificationRuleDto.builder()
                .id(r.getId())
                .name(r.getName())
                .triggerEvent(r.getTriggerEvent())
                .templateId(r.getTemplateId())
                .templateName(templateName)
                .channelType(r.getChannelType())
                .delayDays(r.getDelayDays())
                .delayDirection(r.getDelayDirection())
                .description(r.getDescription())
                .active(r.isActive())
                .createdAt(r.getCreatedAt())
                .build();
    }
}

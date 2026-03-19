package com.gymplatform.modules.communication;

import com.gymplatform.config.multitenancy.TenantContext;
import com.gymplatform.modules.communication.dto.*;
import com.gymplatform.shared.AuditLogService;
import com.gymplatform.shared.BusinessException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
@Slf4j
public class CommunicationTemplateService {

    private final CommunicationTemplateRepository templateRepository;
    private final AuditLogService auditLogService;

    private static final Pattern VARIABLE_PATTERN = Pattern.compile("\\{\\{(\\w+\\.\\w+)}}");

    public Page<CommunicationTemplateDto> getAll(Pageable pageable) {
        return templateRepository.findByActiveTrueOrderByNameAsc(pageable).map(this::toDto);
    }

    public Page<CommunicationTemplateDto> getByChannel(ChannelType channelType, Pageable pageable) {
        return templateRepository.findByChannelTypeAndActiveTrueOrderByNameAsc(channelType, pageable)
                .map(this::toDto);
    }

    public CommunicationTemplateDto getById(UUID id) {
        CommunicationTemplate template = templateRepository.findById(id)
                .orElseThrow(() -> BusinessException.notFound("CommunicationTemplate", id));
        return toDto(template);
    }

    @Transactional
    public CommunicationTemplateDto create(CreateTemplateRequest req, UUID userId) {
        CommunicationTemplate template = CommunicationTemplate.builder()
                .name(req.getName())
                .channelType(req.getChannelType())
                .subject(req.getSubject())
                .bodyHtml(req.getBodyHtml())
                .bodyText(req.getBodyText())
                .category(req.getCategory())
                .locale(req.getLocale())
                .logoUrl(req.getLogoUrl())
                .brandColor(req.getBrandColor())
                .active(true)
                .tenantId(TenantContext.getTenantId())
                .build();
        template = templateRepository.save(template);
        auditLogService.log("CommunicationTemplate", template.getId(), "CREATE", userId, null, null);
        return toDto(template);
    }

    @Transactional
    public CommunicationTemplateDto update(UUID id, CreateTemplateRequest req, UUID userId) {
        CommunicationTemplate template = templateRepository.findById(id)
                .orElseThrow(() -> BusinessException.notFound("CommunicationTemplate", id));
        template.setName(req.getName());
        template.setChannelType(req.getChannelType());
        template.setSubject(req.getSubject());
        template.setBodyHtml(req.getBodyHtml());
        template.setBodyText(req.getBodyText());
        template.setCategory(req.getCategory());
        template.setLocale(req.getLocale());
        template.setLogoUrl(req.getLogoUrl());
        template.setBrandColor(req.getBrandColor());
        template = templateRepository.save(template);
        auditLogService.log("CommunicationTemplate", template.getId(), "UPDATE", userId, null, null);
        return toDto(template);
    }

    @Transactional
    public void deactivate(UUID id, UUID userId) {
        CommunicationTemplate template = templateRepository.findById(id)
                .orElseThrow(() -> BusinessException.notFound("CommunicationTemplate", id));
        template.setActive(false);
        templateRepository.save(template);
        auditLogService.log("CommunicationTemplate", id, "DEACTIVATE", userId, null, null);
    }

    public TemplatePreviewDto preview(UUID templateId, Map<String, String> variables) {
        CommunicationTemplate template = templateRepository.findById(templateId)
                .orElseThrow(() -> BusinessException.notFound("CommunicationTemplate", templateId));
        return TemplatePreviewDto.builder()
                .subject(interpolate(template.getSubject(), variables))
                .bodyHtml(interpolate(template.getBodyHtml(), variables))
                .bodyText(interpolate(template.getBodyText(), variables))
                .build();
    }

    public String interpolate(String text, Map<String, String> variables) {
        if (text == null || variables == null || variables.isEmpty()) return text;
        Matcher matcher = VARIABLE_PATTERN.matcher(text);
        StringBuilder result = new StringBuilder();
        while (matcher.find()) {
            String key = matcher.group(1);
            String replacement = variables.getOrDefault(key, matcher.group(0));
            matcher.appendReplacement(result, Matcher.quoteReplacement(replacement));
        }
        matcher.appendTail(result);
        return result.toString();
    }

    private CommunicationTemplateDto toDto(CommunicationTemplate t) {
        return CommunicationTemplateDto.builder()
                .id(t.getId())
                .name(t.getName())
                .channelType(t.getChannelType())
                .subject(t.getSubject())
                .bodyHtml(t.getBodyHtml())
                .bodyText(t.getBodyText())
                .category(t.getCategory())
                .locale(t.getLocale())
                .logoUrl(t.getLogoUrl())
                .brandColor(t.getBrandColor())
                .active(t.isActive())
                .createdAt(t.getCreatedAt())
                .updatedAt(t.getUpdatedAt())
                .build();
    }
}

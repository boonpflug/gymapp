package com.gymplatform.modules.marketing;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.gymplatform.modules.communication.ChannelType;
import com.gymplatform.modules.communication.CommunicationTemplate;
import com.gymplatform.modules.communication.CommunicationTemplateRepository;
import com.gymplatform.modules.communication.CommunicationTemplateService;
import com.gymplatform.modules.marketing.dto.*;
import com.gymplatform.modules.member.Member;
import com.gymplatform.modules.member.MemberRepository;
import com.gymplatform.shared.AuditLogService;
import com.gymplatform.shared.BusinessException;
import com.gymplatform.config.multitenancy.TenantContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class CampaignService {

    private final CampaignRepository campaignRepository;
    private final CampaignRecipientRepository recipientRepository;
    private final CampaignEventRepository eventRepository;
    private final CommunicationTemplateRepository templateRepository;
    private final CommunicationTemplateService templateService;
    private final AudienceBuilderService audienceBuilderService;
    private final MemberRepository memberRepository;
    private final AuditLogService auditLogService;
    private final RabbitTemplate rabbitTemplate;
    private final ObjectMapper objectMapper;

    @Transactional
    public CampaignDto create(CreateCampaignRequest req, UUID userId) {
        String criteriaJson = null;
        if (req.getAudienceCriteria() != null) {
            try {
                criteriaJson = objectMapper.writeValueAsString(req.getAudienceCriteria());
            } catch (JsonProcessingException e) {
                throw BusinessException.badRequest("Invalid audience criteria");
            }
        }

        Campaign campaign = Campaign.builder()
                .name(req.getName())
                .description(req.getDescription())
                .campaignType(req.getCampaignType())
                .status(CampaignStatus.DRAFT)
                .templateId(req.getTemplateId())
                .subject(req.getSubject())
                .bodyHtml(req.getBodyHtml())
                .bodyText(req.getBodyText())
                .audienceCriteria(criteriaJson)
                .scheduledAt(req.getScheduledAt())
                .totalRecipients(0)
                .sentCount(0)
                .deliveredCount(0)
                .openedCount(0)
                .clickedCount(0)
                .failedCount(0)
                .convertedCount(0)
                .createdBy(userId)
                .tenantId(TenantContext.getTenantId())
                .build();
        campaign = campaignRepository.save(campaign);

        auditLogService.log("Campaign", campaign.getId(), "CREATE", userId, null, null);

        return toDto(campaign);
    }

    @Transactional
    public CampaignDto update(UUID id, CreateCampaignRequest req, UUID userId) {
        Campaign campaign = campaignRepository.findById(id)
                .orElseThrow(() -> BusinessException.notFound("Campaign", id));

        if (campaign.getStatus() != CampaignStatus.DRAFT) {
            throw BusinessException.badRequest("Only draft campaigns can be updated");
        }

        campaign.setName(req.getName());
        campaign.setDescription(req.getDescription());
        campaign.setCampaignType(req.getCampaignType());
        campaign.setTemplateId(req.getTemplateId());
        campaign.setSubject(req.getSubject());
        campaign.setBodyHtml(req.getBodyHtml());
        campaign.setBodyText(req.getBodyText());
        campaign.setScheduledAt(req.getScheduledAt());

        if (req.getAudienceCriteria() != null) {
            try {
                campaign.setAudienceCriteria(objectMapper.writeValueAsString(req.getAudienceCriteria()));
            } catch (JsonProcessingException e) {
                throw BusinessException.badRequest("Invalid audience criteria");
            }
        }

        campaign = campaignRepository.save(campaign);
        auditLogService.log("Campaign", campaign.getId(), "UPDATE", userId, null, null);

        return toDto(campaign);
    }

    public CampaignDto getById(UUID id) {
        Campaign campaign = campaignRepository.findById(id)
                .orElseThrow(() -> BusinessException.notFound("Campaign", id));
        return toDto(campaign);
    }

    public Page<CampaignDto> list(CampaignStatus status, Pageable pageable) {
        if (status != null) {
            return campaignRepository.findByStatusOrderByCreatedAtDesc(status, pageable)
                    .map(this::toDto);
        }
        return campaignRepository.findAllByOrderByCreatedAtDesc(pageable).map(this::toDto);
    }

    @Transactional
    public CampaignDto schedule(UUID id, UUID userId) {
        Campaign campaign = campaignRepository.findById(id)
                .orElseThrow(() -> BusinessException.notFound("Campaign", id));

        if (campaign.getStatus() != CampaignStatus.DRAFT) {
            throw BusinessException.badRequest("Only draft campaigns can be scheduled");
        }

        if (campaign.getScheduledAt() == null) {
            throw BusinessException.badRequest("Scheduled time must be set");
        }

        AudienceCriteria criteria = parseAudienceCriteria(campaign.getAudienceCriteria());
        List<Member> audience = audienceBuilderService.buildAudience(criteria);

        if (audience.isEmpty()) {
            throw BusinessException.badRequest("No recipients match the audience criteria");
        }

        createRecipients(campaign, audience);
        campaign.setTotalRecipients(audience.size());
        campaign.setStatus(CampaignStatus.SCHEDULED);
        campaign = campaignRepository.save(campaign);

        auditLogService.log("Campaign", campaign.getId(), "SCHEDULE", userId, null,
                audience.size() + " recipients");

        return toDto(campaign);
    }

    @Transactional
    public CampaignDto sendNow(UUID id, UUID userId) {
        Campaign campaign = campaignRepository.findById(id)
                .orElseThrow(() -> BusinessException.notFound("Campaign", id));

        if (campaign.getStatus() != CampaignStatus.DRAFT && campaign.getStatus() != CampaignStatus.SCHEDULED) {
            throw BusinessException.badRequest("Campaign cannot be sent in status: " + campaign.getStatus());
        }

        AudienceCriteria criteria = parseAudienceCriteria(campaign.getAudienceCriteria());
        List<Member> audience = audienceBuilderService.buildAudience(criteria);

        if (audience.isEmpty()) {
            throw BusinessException.badRequest("No recipients match the audience criteria");
        }

        if (campaign.getStatus() == CampaignStatus.DRAFT) {
            createRecipients(campaign, audience);
            campaign.setTotalRecipients(audience.size());
        }

        executeCampaignSend(campaign);

        auditLogService.log("Campaign", campaign.getId(), "SEND", userId, null,
                audience.size() + " recipients");

        return toDto(campaign);
    }

    @Transactional
    public CampaignDto cancel(UUID id, UUID userId) {
        Campaign campaign = campaignRepository.findById(id)
                .orElseThrow(() -> BusinessException.notFound("Campaign", id));

        if (campaign.getStatus() == CampaignStatus.SENT) {
            throw BusinessException.badRequest("Cannot cancel a sent campaign");
        }

        campaign.setStatus(CampaignStatus.CANCELLED);
        campaign = campaignRepository.save(campaign);
        auditLogService.log("Campaign", campaign.getId(), "CANCEL", userId, null, null);

        return toDto(campaign);
    }

    public Page<CampaignRecipientDto> getRecipients(UUID campaignId, Pageable pageable) {
        return recipientRepository.findByCampaignIdOrderByCreatedAtDesc(campaignId, pageable)
                .map(this::toRecipientDto);
    }

    public CampaignStatsDto getOverallStats() {
        List<Campaign> allCampaigns = campaignRepository.findAll();
        int total = allCampaigns.size();
        int active = (int) allCampaigns.stream()
                .filter(c -> c.getStatus() == CampaignStatus.SCHEDULED || c.getStatus() == CampaignStatus.SENDING)
                .count();

        long totalSent = allCampaigns.stream().mapToLong(c -> nullSafe(c.getSentCount())).sum();
        long totalDelivered = allCampaigns.stream().mapToLong(c -> nullSafe(c.getDeliveredCount())).sum();
        long totalOpened = allCampaigns.stream().mapToLong(c -> nullSafe(c.getOpenedCount())).sum();
        long totalClicked = allCampaigns.stream().mapToLong(c -> nullSafe(c.getClickedCount())).sum();
        long totalFailed = allCampaigns.stream().mapToLong(c -> nullSafe(c.getFailedCount())).sum();

        return CampaignStatsDto.builder()
                .totalCampaigns(total)
                .activeCampaigns(active)
                .totalSent(totalSent)
                .totalDelivered(totalDelivered)
                .totalOpened(totalOpened)
                .totalClicked(totalClicked)
                .totalFailed(totalFailed)
                .avgDeliveryRate(totalSent > 0 ? (double) totalDelivered / totalSent * 100 : 0)
                .avgOpenRate(totalDelivered > 0 ? (double) totalOpened / totalDelivered * 100 : 0)
                .avgClickRate(totalOpened > 0 ? (double) totalClicked / totalOpened * 100 : 0)
                .build();
    }

    @Scheduled(fixedRate = 60000)
    @Transactional
    public void processScheduledCampaigns() {
        List<Campaign> due = campaignRepository.findByStatusAndScheduledAtLessThanEqual(
                CampaignStatus.SCHEDULED, Instant.now());
        for (Campaign campaign : due) {
            try {
                executeCampaignSend(campaign);
                log.info("Auto-sent scheduled campaign: {}", campaign.getId());
            } catch (Exception e) {
                log.error("Failed to send scheduled campaign {}: {}", campaign.getId(), e.getMessage());
            }
        }
    }

    private void executeCampaignSend(Campaign campaign) {
        campaign.setStatus(CampaignStatus.SENDING);
        campaign.setSentAt(Instant.now());
        campaignRepository.save(campaign);

        List<CampaignRecipient> recipients = recipientRepository.findByCampaignId(campaign.getId());
        int sent = 0;
        int failed = 0;

        ChannelType channelType = mapCampaignTypeToChannel(campaign.getCampaignType());

        for (CampaignRecipient recipient : recipients) {
            try {
                String body = campaign.getBodyHtml() != null ? campaign.getBodyHtml() : campaign.getBodyText();

                if (campaign.getTemplateId() != null) {
                    CommunicationTemplate template = templateRepository.findById(campaign.getTemplateId())
                            .orElse(null);
                    if (template != null) {
                        Member member = memberRepository.findById(recipient.getMemberId()).orElse(null);
                        if (member != null) {
                            Map<String, String> vars = Map.of(
                                    "member.firstName", member.getFirstName(),
                                    "member.lastName", member.getLastName(),
                                    "member.email", member.getEmail()
                            );
                            body = templateService.interpolate(
                                    template.getBodyHtml() != null ? template.getBodyHtml() : template.getBodyText(),
                                    vars);
                        }
                    }
                }

                try {
                    rabbitTemplate.convertAndSend("notification.events", "campaign.message.send", Map.of(
                            "campaignId", campaign.getId().toString(),
                            "recipientId", recipient.getId().toString(),
                            "memberId", recipient.getMemberId().toString(),
                            "channel", channelType.name(),
                            "subject", campaign.getSubject() != null ? campaign.getSubject() : "",
                            "body", body != null ? body : "",
                            "tenantId", TenantContext.getTenantId()
                    ));
                } catch (Exception e) {
                    log.warn("Failed to publish campaign message event: {}", e.getMessage());
                }

                recipient.setStatus(CampaignEventType.SENT);
                recipient.setSentAt(Instant.now());
                recipientRepository.save(recipient);

                logEvent(campaign.getId(), recipient.getId(), recipient.getMemberId(), CampaignEventType.SENT);
                sent++;
            } catch (Exception e) {
                recipient.setStatus(CampaignEventType.FAILED);
                recipient.setErrorMessage(e.getMessage());
                recipientRepository.save(recipient);

                logEvent(campaign.getId(), recipient.getId(), recipient.getMemberId(), CampaignEventType.FAILED);
                failed++;
            }
        }

        campaign.setSentCount(sent);
        campaign.setFailedCount(failed);
        campaign.setDeliveredCount(sent);
        campaign.setStatus(CampaignStatus.SENT);
        campaignRepository.save(campaign);
    }

    private void createRecipients(Campaign campaign, List<Member> audience) {
        ChannelType channelType = mapCampaignTypeToChannel(campaign.getCampaignType());

        for (Member member : audience) {
            String address = resolveAddress(member, channelType);
            CampaignRecipient recipient = CampaignRecipient.builder()
                    .campaignId(campaign.getId())
                    .memberId(member.getId())
                    .recipientAddress(address)
                    .tenantId(TenantContext.getTenantId())
                    .build();
            recipientRepository.save(recipient);
        }
    }

    private String resolveAddress(Member member, ChannelType channelType) {
        return switch (channelType) {
            case EMAIL -> member.getEmail();
            case SMS, WHATSAPP -> member.getPhone();
            case PUSH -> member.getId().toString();
            case LETTER -> member.getEmail();
        };
    }

    private ChannelType mapCampaignTypeToChannel(CampaignType type) {
        return switch (type) {
            case EMAIL -> ChannelType.EMAIL;
            case SMS -> ChannelType.SMS;
            case PUSH -> ChannelType.PUSH;
        };
    }

    private void logEvent(UUID campaignId, UUID recipientId, UUID memberId, CampaignEventType eventType) {
        CampaignEvent event = CampaignEvent.builder()
                .campaignId(campaignId)
                .recipientId(recipientId)
                .memberId(memberId)
                .eventType(eventType)
                .tenantId(TenantContext.getTenantId())
                .build();
        eventRepository.save(event);
    }

    private AudienceCriteria parseAudienceCriteria(String json) {
        if (json == null || json.isBlank()) return null;
        try {
            return objectMapper.readValue(json, AudienceCriteria.class);
        } catch (JsonProcessingException e) {
            log.warn("Failed to parse audience criteria: {}", e.getMessage());
            return null;
        }
    }

    private int nullSafe(Integer val) {
        return val != null ? val : 0;
    }

    private CampaignDto toDto(Campaign c) {
        String templateName = null;
        if (c.getTemplateId() != null) {
            templateName = templateRepository.findById(c.getTemplateId())
                    .map(CommunicationTemplate::getName).orElse(null);
        }

        int sent = nullSafe(c.getSentCount());
        int delivered = nullSafe(c.getDeliveredCount());
        int opened = nullSafe(c.getOpenedCount());
        int clicked = nullSafe(c.getClickedCount());

        return CampaignDto.builder()
                .id(c.getId())
                .name(c.getName())
                .description(c.getDescription())
                .campaignType(c.getCampaignType())
                .status(c.getStatus())
                .templateId(c.getTemplateId())
                .templateName(templateName)
                .subject(c.getSubject())
                .bodyHtml(c.getBodyHtml())
                .bodyText(c.getBodyText())
                .audienceCriteria(c.getAudienceCriteria())
                .scheduledAt(c.getScheduledAt())
                .sentAt(c.getSentAt())
                .totalRecipients(c.getTotalRecipients())
                .sentCount(c.getSentCount())
                .deliveredCount(c.getDeliveredCount())
                .openedCount(c.getOpenedCount())
                .clickedCount(c.getClickedCount())
                .failedCount(c.getFailedCount())
                .convertedCount(c.getConvertedCount())
                .deliveryRate(sent > 0 ? (double) delivered / sent * 100 : null)
                .openRate(delivered > 0 ? (double) opened / delivered * 100 : null)
                .clickRate(opened > 0 ? (double) clicked / opened * 100 : null)
                .createdBy(c.getCreatedBy())
                .createdAt(c.getCreatedAt())
                .updatedAt(c.getUpdatedAt())
                .build();
    }

    private CampaignRecipientDto toRecipientDto(CampaignRecipient r) {
        Member member = memberRepository.findById(r.getMemberId()).orElse(null);
        return CampaignRecipientDto.builder()
                .id(r.getId())
                .campaignId(r.getCampaignId())
                .memberId(r.getMemberId())
                .memberName(member != null ? member.getFirstName() + " " + member.getLastName() : null)
                .memberEmail(member != null ? member.getEmail() : null)
                .recipientAddress(r.getRecipientAddress())
                .status(r.getStatus())
                .sentAt(r.getSentAt())
                .deliveredAt(r.getDeliveredAt())
                .openedAt(r.getOpenedAt())
                .clickedAt(r.getClickedAt())
                .errorMessage(r.getErrorMessage())
                .createdAt(r.getCreatedAt())
                .build();
    }
}

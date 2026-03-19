package com.gymplatform.modules.communication;

import com.gymplatform.config.multitenancy.TenantContext;
import com.gymplatform.modules.communication.dto.MessageStatsDto;
import com.gymplatform.modules.communication.dto.SendMessageRequest;
import com.gymplatform.modules.communication.dto.SentMessageDto;
import com.gymplatform.modules.member.Member;
import com.gymplatform.modules.member.MemberRepository;
import com.gymplatform.shared.AuditLogService;
import com.gymplatform.shared.BusinessException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class MessageService {

    private final SentMessageRepository sentMessageRepository;
    private final CommunicationTemplateRepository templateRepository;
    private final CommunicationTemplateService templateService;
    private final MemberRepository memberRepository;
    private final AuditLogService auditLogService;

    @Transactional
    public SentMessageDto sendMessage(SendMessageRequest req, UUID userId) {
        Member member = memberRepository.findById(req.getMemberId())
                .orElseThrow(() -> BusinessException.notFound("Member", req.getMemberId()));

        CommunicationTemplate template = templateRepository.findById(req.getTemplateId())
                .orElseThrow(() -> BusinessException.notFound("CommunicationTemplate", req.getTemplateId()));

        Map<String, String> variables = req.getVariables() != null ? req.getVariables() : Map.of();

        String resolvedSubject = templateService.interpolate(template.getSubject(), variables);
        String resolvedBody = templateService.interpolate(
                template.getBodyHtml() != null ? template.getBodyHtml() : template.getBodyText(), variables);

        String recipientAddress = resolveRecipientAddress(member, req.getChannelType());

        SentMessage message = SentMessage.builder()
                .memberId(req.getMemberId())
                .templateId(req.getTemplateId())
                .channelType(req.getChannelType())
                .subject(resolvedSubject)
                .bodyPreview(resolvedBody != null ? resolvedBody.substring(0, Math.min(resolvedBody.length(), 1000)) : null)
                .recipientAddress(recipientAddress)
                .status(MessageStatus.PENDING)
                .sentBy(userId)
                .tenantId(TenantContext.getTenantId())
                .build();
        message = sentMessageRepository.save(message);

        // Dispatch to channel
        dispatchMessage(message, resolvedSubject, resolvedBody, recipientAddress);

        auditLogService.log("SentMessage", message.getId(), "SEND", userId, null,
                req.getChannelType().name());
        return toDto(message);
    }

    @Transactional
    public SentMessageDto sendFromRule(UUID memberId, UUID templateId, ChannelType channelType,
                                        String triggerEvent, Map<String, String> variables) {
        Member member = memberRepository.findById(memberId).orElse(null);
        if (member == null) {
            log.warn("Cannot send notification to unknown member: {}", memberId);
            return null;
        }

        CommunicationTemplate template = templateRepository.findById(templateId).orElse(null);
        if (template == null) {
            log.warn("Cannot send notification with unknown template: {}", templateId);
            return null;
        }

        String resolvedSubject = templateService.interpolate(template.getSubject(), variables);
        String resolvedBody = templateService.interpolate(
                template.getBodyHtml() != null ? template.getBodyHtml() : template.getBodyText(), variables);

        String recipientAddress = resolveRecipientAddress(member, channelType);

        SentMessage message = SentMessage.builder()
                .memberId(memberId)
                .templateId(templateId)
                .channelType(channelType)
                .subject(resolvedSubject)
                .bodyPreview(resolvedBody != null ? resolvedBody.substring(0, Math.min(resolvedBody.length(), 1000)) : null)
                .recipientAddress(recipientAddress)
                .status(MessageStatus.PENDING)
                .triggerEvent(triggerEvent)
                .tenantId(TenantContext.getTenantId())
                .build();
        message = sentMessageRepository.save(message);

        dispatchMessage(message, resolvedSubject, resolvedBody, recipientAddress);

        return toDto(message);
    }

    @Transactional
    public SentMessageDto resend(UUID messageId, UUID userId) {
        SentMessage message = sentMessageRepository.findById(messageId)
                .orElseThrow(() -> BusinessException.notFound("SentMessage", messageId));

        if (message.getStatus() != MessageStatus.FAILED) {
            throw BusinessException.badRequest("Only failed messages can be resent");
        }

        message.setStatus(MessageStatus.PENDING);
        message.setErrorMessage(null);
        message.setFailedAt(null);
        message = sentMessageRepository.save(message);

        dispatchMessage(message, message.getSubject(), message.getBodyPreview(), message.getRecipientAddress());

        auditLogService.log("SentMessage", message.getId(), "RESEND", userId, null, null);
        return toDto(message);
    }

    public Page<SentMessageDto> getMemberHistory(UUID memberId, Pageable pageable) {
        return sentMessageRepository.findByMemberIdOrderBySentAtDesc(memberId, pageable).map(this::toDto);
    }

    public Page<SentMessageDto> getAll(Pageable pageable) {
        return sentMessageRepository.findAllByOrderBySentAtDesc(pageable).map(this::toDto);
    }

    public Page<SentMessageDto> getByStatus(MessageStatus status, Pageable pageable) {
        return sentMessageRepository.findByStatusOrderBySentAtDesc(status, pageable).map(this::toDto);
    }

    public MessageStatsDto getStats(int days) {
        Instant since = Instant.now().minus(days, ChronoUnit.DAYS);
        return MessageStatsDto.builder()
                .totalSent(sentMessageRepository.countSince(since))
                .delivered(sentMessageRepository.countByStatusSince(MessageStatus.DELIVERED, since))
                .failed(sentMessageRepository.countByStatusSince(MessageStatus.FAILED, since))
                .opened(sentMessageRepository.countByStatusSince(MessageStatus.OPENED, since))
                .pending(sentMessageRepository.countByStatusSince(MessageStatus.PENDING, since))
                .build();
    }

    private void dispatchMessage(SentMessage message, String subject, String body, String recipientAddress) {
        try {
            switch (message.getChannelType()) {
                case EMAIL -> dispatchEmail(recipientAddress, subject, body, message);
                case SMS -> dispatchSms(recipientAddress, body, message);
                case PUSH -> dispatchPush(message.getMemberId(), subject, body, message);
                case WHATSAPP -> dispatchWhatsApp(recipientAddress, body, message);
                case LETTER -> markAsPending(message);
            }
        } catch (Exception e) {
            log.error("Failed to dispatch message {}: {}", message.getId(), e.getMessage());
            message.setStatus(MessageStatus.FAILED);
            message.setFailedAt(Instant.now());
            message.setErrorMessage(e.getMessage());
            sentMessageRepository.save(message);
        }
    }

    private void dispatchEmail(String to, String subject, String body, SentMessage message) {
        // AWS SES integration point
        log.info("Sending email to {} — subject: {}", to, subject);
        // In production: sesClient.sendEmail(...)
        message.setStatus(MessageStatus.SENT);
        message.setSentAt(Instant.now());
        sentMessageRepository.save(message);
    }

    private void dispatchSms(String to, String body, SentMessage message) {
        // Twilio SMS integration point
        log.info("Sending SMS to {}", to);
        // In production: twilioClient.messages.create(...)
        message.setStatus(MessageStatus.SENT);
        message.setSentAt(Instant.now());
        sentMessageRepository.save(message);
    }

    private void dispatchPush(UUID memberId, String title, String body, SentMessage message) {
        // Firebase Cloud Messaging integration point
        log.info("Sending push notification to member {}", memberId);
        // In production: firebaseMessaging.send(...)
        message.setStatus(MessageStatus.SENT);
        message.setSentAt(Instant.now());
        sentMessageRepository.save(message);
    }

    private void dispatchWhatsApp(String to, String body, SentMessage message) {
        // Twilio WhatsApp API integration point
        log.info("Sending WhatsApp message to {}", to);
        // In production: twilioClient.messages.create(whatsapp:...)
        message.setStatus(MessageStatus.SENT);
        message.setSentAt(Instant.now());
        sentMessageRepository.save(message);
    }

    private void markAsPending(SentMessage message) {
        log.info("Letter queued for PDF generation and printing for member {}", message.getMemberId());
        message.setStatus(MessageStatus.PENDING);
        sentMessageRepository.save(message);
    }

    private String resolveRecipientAddress(Member member, ChannelType channelType) {
        return switch (channelType) {
            case EMAIL -> member.getEmail();
            case SMS, WHATSAPP -> member.getPhone();
            case PUSH -> member.getId().toString();
            case LETTER -> member.getAddress() != null
                    ? member.getAddress().getStreet() + ", " + member.getAddress().getCity()
                    : null;
        };
    }

    private SentMessageDto toDto(SentMessage m) {
        String memberName = memberRepository.findById(m.getMemberId())
                .map(mem -> mem.getFirstName() + " " + mem.getLastName()).orElse(null);
        String templateName = m.getTemplateId() != null
                ? templateRepository.findById(m.getTemplateId()).map(CommunicationTemplate::getName).orElse(null)
                : null;

        return SentMessageDto.builder()
                .id(m.getId())
                .memberId(m.getMemberId())
                .memberName(memberName)
                .templateId(m.getTemplateId())
                .templateName(templateName)
                .channelType(m.getChannelType())
                .subject(m.getSubject())
                .bodyPreview(m.getBodyPreview())
                .recipientAddress(m.getRecipientAddress())
                .status(m.getStatus())
                .sentAt(m.getSentAt())
                .deliveredAt(m.getDeliveredAt())
                .failedAt(m.getFailedAt())
                .openedAt(m.getOpenedAt())
                .errorMessage(m.getErrorMessage())
                .triggerEvent(m.getTriggerEvent())
                .createdAt(m.getCreatedAt())
                .build();
    }
}

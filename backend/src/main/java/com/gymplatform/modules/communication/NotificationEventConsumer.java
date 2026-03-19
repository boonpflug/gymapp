package com.gymplatform.modules.communication;

import com.gymplatform.config.multitenancy.TenantContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Component
@RequiredArgsConstructor
@Slf4j
public class NotificationEventConsumer {

    private final NotificationRuleRepository ruleRepository;
    private final MessageService messageService;

    @RabbitListener(queues = "notification.events.queue")
    public void handleNotificationEvent(Map<String, Object> event) {
        try {
            String eventType = (String) event.get("eventType");
            String tenantId = (String) event.get("tenantId");

            if (eventType == null || tenantId == null) {
                log.warn("Received notification event with missing eventType or tenantId: {}", event);
                return;
            }

            TenantContext.setTenantId(tenantId);
            processEvent(eventType, event);
        } catch (Exception e) {
            log.error("Error processing notification event: {}", e.getMessage(), e);
        } finally {
            TenantContext.clear();
        }
    }

    @RabbitListener(queues = "member.events.notification.queue")
    public void handleMemberEvent(Map<String, Object> event) {
        try {
            String tenantId = (String) event.get("tenantId");
            if (tenantId == null) return;

            TenantContext.setTenantId(tenantId);

            // Determine event type from routing key or event content
            if (event.containsKey("bookingId")) {
                processEvent("booking.created", event);
            } else if (event.containsKey("planId")) {
                processEvent("training.plan.published", event);
            } else if (event.containsKey("sessionId")) {
                processEvent("training.session.completed", event);
            }
        } catch (Exception e) {
            log.error("Error processing member event: {}", e.getMessage(), e);
        } finally {
            TenantContext.clear();
        }
    }

    private void processEvent(String eventType, Map<String, Object> event) {
        TriggerEvent trigger = mapEventToTrigger(eventType);
        if (trigger == null) {
            log.debug("No trigger mapping for event type: {}", eventType);
            return;
        }

        List<NotificationRule> rules = ruleRepository.findByTriggerEventAndActiveTrue(trigger);
        if (rules.isEmpty()) {
            log.debug("No active notification rules for trigger: {}", trigger);
            return;
        }

        UUID memberId = extractMemberId(event);
        if (memberId == null) {
            log.warn("Cannot extract memberId from event: {}", eventType);
            return;
        }

        Map<String, String> variables = buildVariables(event);

        for (NotificationRule rule : rules) {
            if (rule.getDelayDirection() == DelayDirection.IMMEDIATE || rule.getDelayDays() == 0) {
                try {
                    messageService.sendFromRule(memberId, rule.getTemplateId(),
                            rule.getChannelType(), trigger.name(), variables);
                    log.info("Sent notification for rule '{}' to member {}", rule.getName(), memberId);
                } catch (Exception e) {
                    log.error("Failed to send notification for rule '{}': {}", rule.getName(), e.getMessage());
                }
            } else {
                // Delayed notifications would be handled by a scheduled job
                log.info("Delayed notification rule '{}' ({} days {}) — queued for later processing",
                        rule.getName(), rule.getDelayDays(), rule.getDelayDirection());
            }
        }
    }

    private TriggerEvent mapEventToTrigger(String eventType) {
        return switch (eventType) {
            case "member.created" -> TriggerEvent.WELCOME;
            case "payment.failed" -> TriggerEvent.PAYMENT_FAILED;
            case "payment.succeeded" -> TriggerEvent.PAYMENT_SUCCESS;
            case "booking.created" -> TriggerEvent.CLASS_REMINDER;
            case "training.plan.published" -> TriggerEvent.TRAINING_PLAN_PUBLISHED;
            case "contract.cancelled" -> TriggerEvent.CONTRACT_CANCELLATION;
            default -> null;
        };
    }

    private UUID extractMemberId(Map<String, Object> event) {
        Object memberIdObj = event.get("memberId");
        if (memberIdObj == null) return null;
        try {
            return UUID.fromString(memberIdObj.toString());
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    private Map<String, String> buildVariables(Map<String, Object> event) {
        Map<String, String> variables = new HashMap<>();
        event.forEach((key, value) -> {
            if (value != null) {
                variables.put("event." + key, value.toString());
            }
        });
        return variables;
    }
}

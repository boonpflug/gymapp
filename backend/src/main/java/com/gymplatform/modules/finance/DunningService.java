package com.gymplatform.modules.finance;

import com.gymplatform.config.multitenancy.TenantContext;
import com.gymplatform.modules.tenant.Tenant;
import com.gymplatform.modules.tenant.TenantRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class DunningService {

    private final DunningRunRepository dunningRunRepository;
    private final DunningLevelRepository dunningLevelRepository;
    private final InvoiceRepository invoiceRepository;
    private final RabbitTemplate rabbitTemplate;
    private final TenantRepository tenantRepository;

    @Scheduled(cron = "0 0 8 * * *")
    public void processDunning() {
        log.info("Starting dunning process");
        for (Tenant tenant : tenantRepository.findAll()) {
            try {
                TenantContext.setTenantId(tenant.getSchemaName());
                processDunningForTenant();
            } catch (Exception e) {
                log.error("Dunning failed for tenant {}: {}", tenant.getSchemaName(), e.getMessage());
            } finally {
                TenantContext.clear();
            }
        }
        log.info("Dunning process completed");
    }

    @Transactional
    public void processDunningForTenant() {
        List<Invoice> overdueInvoices = invoiceRepository
                .findByStatusIn(List.of(InvoiceStatus.ISSUED, InvoiceStatus.OVERDUE));
        List<DunningLevel> levels = dunningLevelRepository.findAllByOrderByLevelAsc();

        for (Invoice invoice : overdueInvoices) {
            if (invoice.getDueDate() == null || !invoice.getDueDate().isBefore(LocalDate.now())) {
                continue;
            }

            long daysOverdue = LocalDate.now().toEpochDay() - invoice.getDueDate().toEpochDay();

            if (invoice.getStatus() != InvoiceStatus.OVERDUE) {
                invoice.setStatus(InvoiceStatus.OVERDUE);
                invoiceRepository.save(invoice);
            }

            DunningRun dunningRun = dunningRunRepository.findByInvoiceId(invoice.getId())
                    .orElseGet(() -> {
                        DunningRun newRun = DunningRun.builder()
                                .invoiceId(invoice.getId())
                                .memberId(invoice.getMemberId())
                                .currentLevel(0)
                                .resolved(false)
                                .build();
                        return dunningRunRepository.save(newRun);
                    });

            for (DunningLevel level : levels) {
                if (daysOverdue >= level.getDaysAfterDue()
                        && level.getLevel() > dunningRun.getCurrentLevel()) {
                    dunningRun.setCurrentLevel(level.getLevel());
                    dunningRun.setLastEscalatedAt(Instant.now());
                    dunningRunRepository.save(dunningRun);

                    log.info("Dunning escalated to level {} for invoice {}",
                            level.getLevel(), invoice.getInvoiceNumber());

                    try {
                        rabbitTemplate.convertAndSend("notification.events",
                                "notification.dunning",
                                "DUNNING:" + invoice.getMemberId() + ":" + level.getLevel());
                    } catch (Exception e) {
                        log.warn("Failed to send dunning notification", e);
                    }
                }
            }
        }

    }
}

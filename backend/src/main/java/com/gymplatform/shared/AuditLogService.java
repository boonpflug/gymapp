package com.gymplatform.shared;

import com.gymplatform.config.multitenancy.TenantContext;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AuditLogService {

    private final AuditLogRepository auditLogRepository;

    @Async
    public void log(String entityType, UUID entityId, String action, UUID userId,
                    String oldValue, String newValue) {
        AuditLog auditLog = AuditLog.builder()
                .entityType(entityType)
                .entityId(entityId)
                .action(action)
                .userId(userId)
                .oldValue(oldValue)
                .newValue(newValue)
                .tenantId(TenantContext.getTenantId())
                .build();
        auditLogRepository.save(auditLog);
    }
}

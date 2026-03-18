package com.gymplatform.modules.checkin.dto;

import com.gymplatform.modules.checkin.RestrictionReason;
import lombok.Builder;
import lombok.Data;

import java.time.Instant;
import java.util.UUID;

@Data
@Builder
public class AccessRestrictionDto {
    private UUID id;
    private UUID memberId;
    private RestrictionReason reason;
    private String description;
    private boolean active;
    private UUID restrictedBy;
    private Instant restrictedAt;
    private Instant releasedAt;
    private UUID releasedBy;
    private boolean autoRelease;
}

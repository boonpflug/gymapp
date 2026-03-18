package com.gymplatform.modules.checkin;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class CheckInResult {
    private boolean allowed;
    private String denialReason;
    private boolean gateOpened;
}

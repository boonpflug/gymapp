package com.gymplatform.modules.contract.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data @NoArgsConstructor @AllArgsConstructor
public class RenewalSettingsRequest {
    private Integer renewalTermMonths;
    private Integer renewalNoticeDays;
}

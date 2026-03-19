package com.gymplatform.modules.marketing.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class CampaignStatsDto {
    private int totalCampaigns;
    private int activeCampaigns;
    private long totalSent;
    private long totalDelivered;
    private long totalOpened;
    private long totalClicked;
    private long totalFailed;
    private double avgDeliveryRate;
    private double avgOpenRate;
    private double avgClickRate;
}

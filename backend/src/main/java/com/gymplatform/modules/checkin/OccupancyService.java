package com.gymplatform.modules.checkin;

import com.gymplatform.modules.checkin.dto.OccupancyDto;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

@Service
@RequiredArgsConstructor
public class OccupancyService {

    private static final int OCCUPANCY_WINDOW_HOURS = 18;

    private final CheckInRepository checkInRepository;
    private final SimpMessagingTemplate messagingTemplate;

    public OccupancyDto getCurrentOccupancy(Integer maxCapacity) {
        Instant since = Instant.now().minus(OCCUPANCY_WINDOW_HOURS, ChronoUnit.HOURS);
        long count = checkInRepository.countCurrentOccupancy(since);
        boolean atCapacity = maxCapacity != null && count >= maxCapacity;

        return OccupancyDto.builder()
                .currentCount(count)
                .maxCapacity(maxCapacity)
                .atCapacity(atCapacity)
                .build();
    }

    public boolean isAtCapacity(Integer maxCapacity) {
        if (maxCapacity == null) return false;
        Instant since = Instant.now().minus(OCCUPANCY_WINDOW_HOURS, ChronoUnit.HOURS);
        long count = checkInRepository.countCurrentOccupancy(since);
        return count >= maxCapacity;
    }

    public void broadcastOccupancyUpdate(String tenantId, Integer maxCapacity) {
        OccupancyDto occupancy = getCurrentOccupancy(maxCapacity);
        messagingTemplate.convertAndSend(
                "/topic/occupancy/" + tenantId,
                occupancy
        );
    }
}

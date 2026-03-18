package com.gymplatform.modules.checkin;

import com.gymplatform.modules.checkin.dto.AccessEventDto;
import com.gymplatform.modules.member.Member;
import com.gymplatform.modules.member.MemberRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AccessEventService {

    private final AccessEventRepository eventRepository;
    private final AccessDeviceRepository deviceRepository;
    private final MemberRepository memberRepository;

    public Page<AccessEventDto> getRecentEvents(Pageable pageable) {
        return eventRepository.findAllByOrderByCreatedAtDesc(pageable)
                .map(this::toDto);
    }

    public Page<AccessEventDto> getEventsByDevice(UUID deviceId, Pageable pageable) {
        return eventRepository.findByDeviceIdOrderByCreatedAtDesc(deviceId, pageable)
                .map(this::toDto);
    }

    public Page<AccessEventDto> getEventsByMember(UUID memberId, Pageable pageable) {
        return eventRepository.findByMemberIdOrderByCreatedAtDesc(memberId, pageable)
                .map(this::toDto);
    }

    private AccessEventDto toDto(AccessEvent e) {
        String deviceName = null;
        if (e.getDeviceId() != null) {
            deviceName = deviceRepository.findById(e.getDeviceId())
                    .map(AccessDevice::getName)
                    .orElse(null);
        }
        String memberName = null;
        if (e.getMemberId() != null) {
            memberName = memberRepository.findById(e.getMemberId())
                    .map(m -> m.getFirstName() + " " + m.getLastName())
                    .orElse(null);
        }

        return AccessEventDto.builder()
                .id(e.getId())
                .deviceId(e.getDeviceId())
                .deviceName(deviceName)
                .memberId(e.getMemberId())
                .memberName(memberName)
                .eventType(e.getEventType())
                .reasonCode(e.getReasonCode())
                .details(e.getDetails())
                .createdAt(e.getCreatedAt())
                .build();
    }
}

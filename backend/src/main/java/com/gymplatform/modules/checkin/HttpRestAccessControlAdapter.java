package com.gymplatform.modules.checkin;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

/**
 * Access control adapter for IP-connected devices using HTTP REST API.
 * Communicates with door controllers, turnstiles, and gates via their HTTP endpoints.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class HttpRestAccessControlAdapter implements AccessControlAdapter {

    private final AccessDeviceRepository deviceRepository;
    private final RestTemplate restTemplate;

    @Override
    public CheckInResult processCheckIn(UUID memberId, UUID deviceId) {
        AccessDevice device = deviceRepository.findById(deviceId).orElse(null);
        if (device == null || !device.isActive()) {
            return CheckInResult.builder()
                    .allowed(false)
                    .denialReason("Device not found or inactive")
                    .gateOpened(false)
                    .build();
        }

        try {
            String url = device.getApiEndpoint() + "/access/grant";
            Map<String, String> body = Map.of(
                    "memberId", memberId.toString(),
                    "action", "ENTRY"
            );
            ResponseEntity<Map> response = restTemplate.postForEntity(url, body, Map.class);

            boolean opened = response.getStatusCode().is2xxSuccessful();
            return CheckInResult.builder()
                    .allowed(opened)
                    .gateOpened(opened)
                    .denialReason(opened ? null : "Device rejected access")
                    .build();
        } catch (Exception e) {
            log.error("Failed to communicate with device {}: {}", deviceId, e.getMessage());
            return CheckInResult.builder()
                    .allowed(true)
                    .gateOpened(false)
                    .denialReason("Device communication failed — access granted by policy")
                    .build();
        }
    }

    @Override
    public void processCheckOut(UUID memberId, UUID deviceId) {
        AccessDevice device = deviceRepository.findById(deviceId).orElse(null);
        if (device == null || !device.isActive()) return;

        try {
            String url = device.getApiEndpoint() + "/access/exit";
            Map<String, String> body = Map.of(
                    "memberId", memberId.toString(),
                    "action", "EXIT"
            );
            restTemplate.postForEntity(url, body, Map.class);
        } catch (Exception e) {
            log.error("Failed to send check-out to device {}: {}", deviceId, e.getMessage());
        }
    }

    @Override
    public DeviceStatus getDeviceStatus(UUID deviceId) {
        AccessDevice device = deviceRepository.findById(deviceId).orElse(null);
        if (device == null) {
            return DeviceStatus.builder().online(false).errorMessage("Device not found").build();
        }

        try {
            String url = device.getApiEndpoint() + "/status";
            ResponseEntity<Map> response = restTemplate.getForEntity(url, Map.class);
            boolean online = response.getStatusCode().is2xxSuccessful();

            if (online) {
                device.setLastHeartbeatAt(Instant.now());
                deviceRepository.save(device);
            }

            return DeviceStatus.builder()
                    .online(online)
                    .lastHeartbeat(device.getLastHeartbeatAt())
                    .build();
        } catch (Exception e) {
            return DeviceStatus.builder()
                    .online(false)
                    .lastHeartbeat(device.getLastHeartbeatAt())
                    .errorMessage(e.getMessage())
                    .build();
        }
    }

    @Override
    public boolean supportsDevice(DeviceType deviceType) {
        return deviceType == DeviceType.DOOR_CONTROLLER
                || deviceType == DeviceType.GATE
                || deviceType == DeviceType.TURNSTILE;
    }
}

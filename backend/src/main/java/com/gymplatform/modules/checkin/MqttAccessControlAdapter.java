package com.gymplatform.modules.checkin;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.UUID;

/**
 * Access control adapter for IoT devices using MQTT protocol.
 * Handles RFID readers, QR scanners, vending machines, body composition scales.
 *
 * NOTE: Full MQTT integration requires Eclipse Paho or Spring Integration MQTT.
 * This implementation provides the adapter interface and logs actions.
 * Wire in an MqttTemplate when MQTT broker is configured.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class MqttAccessControlAdapter implements AccessControlAdapter {

    private final AccessDeviceRepository deviceRepository;

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

        log.info("MQTT check-in: publishing to topic devices/{}/access/grant for member {}",
                deviceId, memberId);

        // In production: mqttTemplate.publish("devices/" + deviceId + "/access/grant", payload)
        return CheckInResult.builder()
                .allowed(true)
                .gateOpened(true)
                .build();
    }

    @Override
    public void processCheckOut(UUID memberId, UUID deviceId) {
        log.info("MQTT check-out: publishing to topic devices/{}/access/exit for member {}",
                deviceId, memberId);
    }

    @Override
    public DeviceStatus getDeviceStatus(UUID deviceId) {
        AccessDevice device = deviceRepository.findById(deviceId).orElse(null);
        if (device == null) {
            return DeviceStatus.builder().online(false).errorMessage("Device not found").build();
        }

        return DeviceStatus.builder()
                .online(device.getLastHeartbeatAt() != null
                        && device.getLastHeartbeatAt().isAfter(
                        java.time.Instant.now().minusSeconds(120)))
                .lastHeartbeat(device.getLastHeartbeatAt())
                .build();
    }

    @Override
    public boolean supportsDevice(DeviceType deviceType) {
        return deviceType == DeviceType.RFID_READER
                || deviceType == DeviceType.QR_SCANNER
                || deviceType == DeviceType.VENDING_MACHINE
                || deviceType == DeviceType.BODY_COMPOSITION_SCALE;
    }
}

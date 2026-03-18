package com.gymplatform.modules.checkin;

import com.gymplatform.config.multitenancy.TenantContext;
import com.gymplatform.modules.checkin.dto.AccessDeviceDto;
import com.gymplatform.modules.checkin.dto.CreateAccessDeviceRequest;
import com.gymplatform.shared.BusinessException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AccessDeviceService {

    private final AccessDeviceRepository deviceRepository;
    private final List<AccessControlAdapter> accessControlAdapters;

    public List<AccessDeviceDto> getAllDevices() {
        return deviceRepository.findAll().stream()
                .map(this::toDto)
                .toList();
    }

    public List<AccessDeviceDto> getActiveDevices() {
        return deviceRepository.findByActiveTrue().stream()
                .map(this::toDto)
                .toList();
    }

    public AccessDeviceDto getDevice(UUID deviceId) {
        AccessDevice device = deviceRepository.findById(deviceId)
                .orElseThrow(() -> BusinessException.notFound("Device", deviceId));
        return toDto(device);
    }

    @Transactional
    public AccessDeviceDto createDevice(CreateAccessDeviceRequest request) {
        AccessDevice device = AccessDevice.builder()
                .name(request.getName())
                .deviceType(request.getDeviceType())
                .mode(request.getMode())
                .locationDescription(request.getLocationDescription())
                .ipAddress(request.getIpAddress())
                .apiEndpoint(request.getApiEndpoint())
                .apiKey(request.getApiKey())
                .maxOccupancy(request.getMaxOccupancy())
                .active(true)
                .tenantId(TenantContext.getTenantId())
                .build();

        device = deviceRepository.save(device);
        return toDto(device);
    }

    @Transactional
    public AccessDeviceDto updateDevice(UUID deviceId, CreateAccessDeviceRequest request) {
        AccessDevice device = deviceRepository.findById(deviceId)
                .orElseThrow(() -> BusinessException.notFound("Device", deviceId));

        device.setName(request.getName());
        device.setDeviceType(request.getDeviceType());
        device.setMode(request.getMode());
        device.setLocationDescription(request.getLocationDescription());
        device.setIpAddress(request.getIpAddress());
        device.setApiEndpoint(request.getApiEndpoint());
        device.setApiKey(request.getApiKey());
        device.setMaxOccupancy(request.getMaxOccupancy());

        device = deviceRepository.save(device);
        return toDto(device);
    }

    @Transactional
    public void toggleDevice(UUID deviceId, boolean active) {
        AccessDevice device = deviceRepository.findById(deviceId)
                .orElseThrow(() -> BusinessException.notFound("Device", deviceId));
        device.setActive(active);
        deviceRepository.save(device);
    }

    public DeviceStatus getDeviceStatus(UUID deviceId) {
        AccessDevice device = deviceRepository.findById(deviceId)
                .orElseThrow(() -> BusinessException.notFound("Device", deviceId));

        AccessControlAdapter adapter = accessControlAdapters.stream()
                .filter(a -> a.supportsDevice(device.getDeviceType()))
                .findFirst()
                .orElse(null);

        if (adapter != null) {
            return adapter.getDeviceStatus(deviceId);
        }

        return DeviceStatus.builder()
                .online(device.isActive())
                .lastHeartbeat(device.getLastHeartbeatAt())
                .build();
    }

    private AccessDeviceDto toDto(AccessDevice d) {
        return AccessDeviceDto.builder()
                .id(d.getId())
                .name(d.getName())
                .deviceType(d.getDeviceType())
                .mode(d.getMode())
                .locationDescription(d.getLocationDescription())
                .ipAddress(d.getIpAddress())
                .apiEndpoint(d.getApiEndpoint())
                .maxOccupancy(d.getMaxOccupancy())
                .active(d.isActive())
                .lastHeartbeatAt(d.getLastHeartbeatAt())
                .build();
    }
}

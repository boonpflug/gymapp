package com.gymplatform.modules.checkin;

import java.util.UUID;

/**
 * Hardware abstraction layer for access control devices.
 * Implementations handle different vendor APIs (doors, gates, turnstiles, etc.).
 */
public interface AccessControlAdapter {

    CheckInResult processCheckIn(UUID memberId, UUID deviceId);

    void processCheckOut(UUID memberId, UUID deviceId);

    DeviceStatus getDeviceStatus(UUID deviceId);

    boolean supportsDevice(DeviceType deviceType);
}

package com.gymplatform.modules.checkin;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface AccessDeviceRepository extends JpaRepository<AccessDevice, UUID> {

    List<AccessDevice> findByActiveTrue();

    List<AccessDevice> findByDeviceType(DeviceType deviceType);
}

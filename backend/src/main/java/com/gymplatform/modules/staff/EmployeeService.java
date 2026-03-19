package com.gymplatform.modules.staff;

import com.gymplatform.config.multitenancy.TenantContext;
import com.gymplatform.modules.staff.dto.CreateEmployeeRequest;
import com.gymplatform.modules.staff.dto.EmployeeDto;
import com.gymplatform.shared.AuditLogService;
import com.gymplatform.shared.BusinessException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class EmployeeService {

    private final EmployeeRepository employeeRepository;
    private final EmployeeFacilityRepository facilityRepository;
    private final AuditLogService auditLogService;

    public Page<EmployeeDto> getAll(Pageable pageable) {
        return employeeRepository.findByActiveTrueOrderByLastNameAsc(pageable).map(this::toDto);
    }

    public EmployeeDto getById(UUID id) {
        Employee emp = employeeRepository.findById(id)
                .orElseThrow(() -> BusinessException.notFound("Employee", id));
        return toDto(emp);
    }

    @Transactional
    public EmployeeDto create(CreateEmployeeRequest req, UUID userId) {
        Employee emp = Employee.builder()
                .firstName(req.getFirstName())
                .lastName(req.getLastName())
                .email(req.getEmail())
                .phone(req.getPhone())
                .role(req.getRole())
                .employmentType(req.getEmploymentType())
                .position(req.getPosition())
                .hourlyRate(req.getHourlyRate())
                .monthlySalary(req.getMonthlySalary())
                .hireDate(req.getHireDate())
                .competencies(req.getCompetencies())
                .active(true)
                .tenantId(TenantContext.getTenantId())
                .build();
        emp = employeeRepository.save(emp);

        if (req.getFacilityIds() != null) {
            saveFacilities(emp.getId(), req.getFacilityIds());
        }

        auditLogService.log("Employee", emp.getId(), "CREATE", userId, null, null);
        return toDto(emp);
    }

    @Transactional
    public EmployeeDto update(UUID id, CreateEmployeeRequest req, UUID userId) {
        Employee emp = employeeRepository.findById(id)
                .orElseThrow(() -> BusinessException.notFound("Employee", id));
        emp.setFirstName(req.getFirstName());
        emp.setLastName(req.getLastName());
        emp.setEmail(req.getEmail());
        emp.setPhone(req.getPhone());
        emp.setRole(req.getRole());
        emp.setEmploymentType(req.getEmploymentType());
        emp.setPosition(req.getPosition());
        emp.setHourlyRate(req.getHourlyRate());
        emp.setMonthlySalary(req.getMonthlySalary());
        emp.setHireDate(req.getHireDate());
        emp.setCompetencies(req.getCompetencies());
        emp = employeeRepository.save(emp);

        if (req.getFacilityIds() != null) {
            facilityRepository.deleteByEmployeeId(id);
            saveFacilities(id, req.getFacilityIds());
        }

        auditLogService.log("Employee", emp.getId(), "UPDATE", userId, null, null);
        return toDto(emp);
    }

    @Transactional
    public void deactivate(UUID id, UUID userId) {
        Employee emp = employeeRepository.findById(id)
                .orElseThrow(() -> BusinessException.notFound("Employee", id));
        emp.setActive(false);
        employeeRepository.save(emp);
        auditLogService.log("Employee", id, "DEACTIVATE", userId, null, null);
    }

    private void saveFacilities(UUID employeeId, List<UUID> facilityIds) {
        for (int i = 0; i < facilityIds.size(); i++) {
            EmployeeFacility ef = EmployeeFacility.builder()
                    .employeeId(employeeId)
                    .facilityId(facilityIds.get(i))
                    .primary(i == 0)
                    .tenantId(TenantContext.getTenantId())
                    .build();
            facilityRepository.save(ef);
        }
    }

    String getEmployeeName(UUID employeeId) {
        return employeeRepository.findById(employeeId)
                .map(e -> e.getFirstName() + " " + e.getLastName())
                .orElse(null);
    }

    private EmployeeDto toDto(Employee e) {
        List<UUID> facilityIds = facilityRepository.findByEmployeeId(e.getId())
                .stream().map(EmployeeFacility::getFacilityId).collect(Collectors.toList());

        return EmployeeDto.builder()
                .id(e.getId())
                .userId(e.getUserId())
                .firstName(e.getFirstName())
                .lastName(e.getLastName())
                .email(e.getEmail())
                .phone(e.getPhone())
                .role(e.getRole())
                .employmentType(e.getEmploymentType())
                .position(e.getPosition())
                .hourlyRate(e.getHourlyRate())
                .monthlySalary(e.getMonthlySalary())
                .hireDate(e.getHireDate())
                .terminationDate(e.getTerminationDate())
                .competencies(e.getCompetencies())
                .active(e.isActive())
                .facilityIds(facilityIds)
                .createdAt(e.getCreatedAt())
                .build();
    }
}

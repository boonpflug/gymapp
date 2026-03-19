package com.gymplatform.modules.staff.dto;

import com.gymplatform.modules.staff.EmploymentType;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Data
@Builder
public class EmployeeDto {
    private UUID id;
    private UUID userId;
    private String firstName;
    private String lastName;
    private String email;
    private String phone;
    private String role;
    private EmploymentType employmentType;
    private String position;
    private BigDecimal hourlyRate;
    private BigDecimal monthlySalary;
    private LocalDate hireDate;
    private LocalDate terminationDate;
    private String competencies;
    private boolean active;
    private List<UUID> facilityIds;
    private Instant createdAt;
}

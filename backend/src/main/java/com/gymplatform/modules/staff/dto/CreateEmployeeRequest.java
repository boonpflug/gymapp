package com.gymplatform.modules.staff.dto;

import com.gymplatform.modules.staff.EmploymentType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Data
public class CreateEmployeeRequest {
    @NotBlank(message = "First name is required")
    private String firstName;
    @NotBlank(message = "Last name is required")
    private String lastName;
    private String email;
    private String phone;
    private String role;
    @NotNull(message = "Employment type is required")
    private EmploymentType employmentType;
    private String position;
    private BigDecimal hourlyRate;
    private BigDecimal monthlySalary;
    private LocalDate hireDate;
    private String competencies;
    private List<UUID> facilityIds;
}

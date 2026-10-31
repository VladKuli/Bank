package com.bank.bank_system.web.admin;

import com.bank.bank_system.domain.Employee;
import com.bank.bank_system.domain.enums.EmployeeRole;
import java.time.Instant;
import java.util.UUID;

public record EmployeeResponse(UUID id, String username, EmployeeRole role, boolean enabled, Instant createdAt) {

    public static EmployeeResponse from(Employee employee) {
        return new EmployeeResponse(
                employee.getId(),
                employee.getUsername(),
                employee.getRole(),
                employee.isEnabled(),
                employee.getCreatedAt());
    }
}

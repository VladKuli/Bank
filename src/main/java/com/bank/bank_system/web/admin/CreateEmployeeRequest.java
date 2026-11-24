package com.bank.bank_system.web.admin;

import com.bank.bank_system.domain.enums.EmployeeRole;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CreateEmployeeRequest(
        @NotBlank @Size(max = 100) String username,
        @NotBlank @Size(min = 12, max = 72) String password,
        @NotNull EmployeeRole role) {
}

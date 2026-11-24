package com.bank.bank_system.service;

import com.bank.bank_system.domain.Employee;
import com.bank.bank_system.domain.enums.EmployeeRole;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.springframework.validation.annotation.Validated;

import java.util.List;
import java.util.UUID;

@Validated
public interface EmployeeService {

    Employee create(@NotBlank String username,
                    @NotBlank String rawPassword,
                    @NotNull EmployeeRole role);

    List<Employee> list();

    Employee setEnabled(@NotNull UUID employeeId,
                        @NotNull Boolean enabled,
                        @NotBlank String actingUsername);

    }

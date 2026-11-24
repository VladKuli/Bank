package com.bank.bank_system.service.impl;

import com.bank.bank_system.domain.Employee;
import com.bank.bank_system.domain.enums.EmployeeRole;
import com.bank.bank_system.exception.LastEnabledAdministratorException;
import com.bank.bank_system.exception.SelfDisableException;
import com.bank.bank_system.exception.DuplicateEmployeeUsernameException;
import com.bank.bank_system.exception.EmployeeNotFoundException;
import com.bank.bank_system.infrastructure.EmployeeRepository;

import java.util.List;
import java.util.Locale;
import java.util.UUID;

import com.bank.bank_system.service.EmployeeService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class EmployeeServiceImpl implements EmployeeService {

    private final EmployeeRepository employeeRepository;
    private final PasswordEncoder passwordEncoder;

    @Transactional
    public Employee create(String username, String rawPassword, EmployeeRole role) {
        String normalizedUsername = username.trim().toLowerCase(Locale.ROOT);
        if (employeeRepository.existsByUsernameIgnoreCase(normalizedUsername)) {
            throw new DuplicateEmployeeUsernameException(normalizedUsername);
        }
        return employeeRepository.save(new Employee(
                normalizedUsername,
                passwordEncoder.encode(rawPassword),
                role));
    }

    @Transactional(readOnly = true)
    public List<Employee> list() {
        return employeeRepository.findAllByOrderByUsernameAsc();
    }

    @Transactional
    public Employee setEnabled(UUID employeeId, Boolean enabled, String actingUsername) {
        Employee employee = employeeRepository.findById(employeeId)
                .orElseThrow(() -> new EmployeeNotFoundException(employeeId));
        if (!enabled && employee.getUsername().equalsIgnoreCase(actingUsername)) {
            throw new SelfDisableException();
        }
        if (!enabled && employee.getRole() == EmployeeRole.ADMIN
                && employeeRepository.countByRoleAndEnabledTrue(EmployeeRole.ADMIN) <= 1) {
            throw new LastEnabledAdministratorException();
        }
        employee.setEnabled(enabled);
        return employee;
    }

}

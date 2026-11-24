package com.bank.bank_system.exception;

import java.util.UUID;

public class EmployeeNotFoundException extends RuntimeException {

    public EmployeeNotFoundException(UUID employeeId) {
        super("Employee '" + employeeId + "' was not found");
    }
}

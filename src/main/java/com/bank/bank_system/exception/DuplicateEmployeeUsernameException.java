package com.bank.bank_system.exception;

public class DuplicateEmployeeUsernameException extends RuntimeException {

    public DuplicateEmployeeUsernameException(String username) {
        super("An employee with username '" + username + "' already exists");
    }
}

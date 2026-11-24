package com.bank.bank_system.exception;

public class LastEnabledAdministratorException extends RuntimeException {

    public LastEnabledAdministratorException() {
        super("The last enabled administrator cannot be disabled");
    }
}

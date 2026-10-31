package com.bank.bank_system.exception;

import java.util.UUID;

public class AccountNotFoundException extends RuntimeException {

    public AccountNotFoundException(UUID id) {
        super("Account '" + id + "' was not found");
    }
}

package com.bank.bank_system.exception;

public class AccountNotOpenException extends RuntimeException {

    public AccountNotOpenException(String accountNumber) {
        super("Account '" + accountNumber + "' is not open");
    }
}

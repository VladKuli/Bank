package com.bank.bank_system.exception;

public class InsufficientFundsException extends RuntimeException {

    public InsufficientFundsException() {
        super("Account balance is insufficient for this operation");
    }
}

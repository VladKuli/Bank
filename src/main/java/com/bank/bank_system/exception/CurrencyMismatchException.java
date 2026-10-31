package com.bank.bank_system.exception;

public class CurrencyMismatchException extends RuntimeException {

    public CurrencyMismatchException() {
        super("Transfers between accounts with different currencies are not supported");
    }
}

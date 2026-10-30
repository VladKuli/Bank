package com.bank.bank_system.exception;

public class InvalidAmountException extends RuntimeException {

    public InvalidAmountException() {
        super("Amount must be positive and have no more than two decimal places");
    }
}

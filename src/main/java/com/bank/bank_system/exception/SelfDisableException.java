package com.bank.bank_system.exception;

public class SelfDisableException extends RuntimeException {

    public SelfDisableException() {
        super("An employee cannot disable their own account");
    }
}

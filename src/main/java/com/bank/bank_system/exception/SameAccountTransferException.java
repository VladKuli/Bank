package com.bank.bank_system.exception;

public class SameAccountTransferException extends RuntimeException {

    public SameAccountTransferException() {
        super("Source and destination accounts must be different");
    }
}

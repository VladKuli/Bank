package com.bank.bank_system.exception;

public class BlockedClientOperationException extends RuntimeException {

    public BlockedClientOperationException() {
        super("Financial operations are not allowed for blocked clients");
    }
}

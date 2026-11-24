package com.bank.bank_system.exception;

public class AccountBalanceMustBeZeroException extends RuntimeException {

    public AccountBalanceMustBeZeroException() {
        super("An account can be closed only when its balance is zero");
    }
}

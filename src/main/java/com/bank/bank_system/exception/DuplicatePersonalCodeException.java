package com.bank.bank_system.exception;

public class DuplicatePersonalCodeException extends RuntimeException {

    public DuplicatePersonalCodeException(String personalCode) {
        super("A client with personal code '" + personalCode + "' already exists");
    }
}

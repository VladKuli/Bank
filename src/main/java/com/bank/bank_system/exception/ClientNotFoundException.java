 package com.bank.bank_system.exception;

import java.util.UUID;

public class ClientNotFoundException extends RuntimeException {

    public ClientNotFoundException(UUID id) {
        super("Client '" + id + "' was not found");
    }
}

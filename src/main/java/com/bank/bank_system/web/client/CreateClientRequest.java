package com.bank.bank_system.web.client;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateClientRequest(
        @NotBlank @Size(max = 100) String firstName,
        @NotBlank @Size(max = 100) String lastName,
        @NotBlank @Size(max = 32) String personalCode,
        @Size(max = 32) String phone,
        @Email @Size(max = 254) String email) {
}

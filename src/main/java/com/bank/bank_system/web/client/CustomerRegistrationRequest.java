package com.bank.bank_system.web.client;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CustomerRegistrationRequest(
        @NotBlank @Size(max = 100) String firstName,
        @NotBlank @Size(max = 100) String lastName,
        @NotBlank @Email @Size(max = 254) String email,
        @Size(max = 32) String phone,
        @NotBlank @Size(min = 12, max = 72) String password,
        @NotBlank String confirmPassword,
        @AssertTrue boolean acceptTerms) {
}

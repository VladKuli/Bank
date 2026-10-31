package com.bank.bank_system.web.client;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record SetCustomerAccountStatusRequest(
        @NotBlank @Pattern(regexp = "OPEN|FROZEN") String status) {
}

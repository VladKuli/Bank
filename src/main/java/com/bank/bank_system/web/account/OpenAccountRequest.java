package com.bank.bank_system.web.account;

import com.bank.bank_system.domain.enums.AccountType;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

public record OpenAccountRequest(
        @NotNull AccountType type,
        @NotNull @Pattern(regexp = "[A-Za-z]{3}") String currency) {
}

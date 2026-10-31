package com.bank.bank_system.web.account;

import com.bank.bank_system.domain.Account;
import com.bank.bank_system.domain.enums.AccountStatus;
import com.bank.bank_system.domain.enums.AccountType;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record AccountResponse(
        UUID id,
        String accountNumber,
        UUID clientId,
        AccountType type,
        String currency,
        BigDecimal balance,
        AccountStatus status,
        Instant openedAt) {

    public static AccountResponse from(Account account) {
        return new AccountResponse(
                account.getId(),
                account.getAccountNumber(),
                account.getOwner().getId(),
                account.getType(),
                account.getCurrency(),
                account.getBalance(),
                account.getStatus(),
                account.getOpenedAt());
    }
}

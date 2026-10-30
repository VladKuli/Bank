package com.bank.bank_system.service;

import com.bank.bank_system.domain.Account;
import com.bank.bank_system.domain.enums.AccountType;
import com.bank.bank_system.domain.FinancialOperation;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.springframework.validation.annotation.Validated;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Validated
public interface AccountService {

    Account open(@NotNull UUID clientId,
                 @NotNull AccountType type,
                 @NotBlank String currency);

    List<Account> listForClient(@NotNull UUID clientId);

    Account get(@NotNull UUID id);

    Account close(@NotNull UUID id);

    FinancialOperation deposit(@NotNull UUID accountId, @NotNull BigDecimal amount);

    FinancialOperation withdraw(@NotNull UUID accountId, @NotNull BigDecimal amount);

    FinancialOperation transfer(@NotNull UUID sourceId,
                                @NotNull UUID targetId,
                                @NotNull BigDecimal amount);

    FinancialOperation transfer(@NotNull UUID sourceId,
                                @NotNull UUID targetId,
                                @NotNull BigDecimal amount,
                                String description);

    FinancialOperation transfer(@NotNull UUID sourceId,
                                @NotNull UUID targetId,
                                @NotNull BigDecimal amount,
                                String description,
                                UUID idempotencyKey);

    List<FinancialOperation> history(@NotNull UUID accountId);

}


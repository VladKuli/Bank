package com.bank.bank_system.web.client;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.util.UUID;

public record CreateTransferRequest(@NotNull UUID sourceAccountId, @NotNull UUID targetAccountId, @NotNull UUID idempotencyKey,
                                    @NotNull @DecimalMin("0.01") @Digits(integer = 17, fraction = 2) BigDecimal amount,
                                    @Size(max = 140) String description) {}

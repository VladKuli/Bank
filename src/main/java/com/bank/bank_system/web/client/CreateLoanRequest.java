package com.bank.bank_system.web.client;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;

public record CreateLoanRequest(@DecimalMin("500.00") @DecimalMax("100000.00") @Digits(integer = 6, fraction = 2) BigDecimal amount,
                                @Positive int termMonths, @NotBlank @Size(max = 300) String purpose) {}

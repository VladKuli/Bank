package com.bank.bank_system.web.operartions;

import com.bank.bank_system.domain.FinancialOperation;
import com.bank.bank_system.domain.enums.FinancialOperationType;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record FinancialOperationResponse(
        UUID id,
        FinancialOperationType type,
        String sourceAccountNumber,
        String targetAccountNumber,
        BigDecimal amount,
        String currency,
        Instant occurredAt,
        String reference,
        String description,
        String status) {

    public static FinancialOperationResponse from(FinancialOperation operation) {
        return new FinancialOperationResponse(
                operation.getId(),
                operation.getType(),
                operation.getSourceAccount() == null ? null : operation.getSourceAccount().getAccountNumber(),
                operation.getTargetAccount() == null ? null : operation.getTargetAccount().getAccountNumber(),
                operation.getAmount(),
                operation.getCurrency(),
                operation.getOccurredAt(),
                operation.getReference(),
                operation.getDescription(),
                operation.getStatus());
    }
}

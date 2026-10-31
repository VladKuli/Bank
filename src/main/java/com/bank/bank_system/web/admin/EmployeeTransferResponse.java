package com.bank.bank_system.web.admin;

import com.bank.bank_system.domain.FinancialOperation;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record EmployeeTransferResponse(UUID id, String reference, String sender, String recipient,
                                       String sourceAccount, String destinationAccount, BigDecimal amount,
                                       String currency,
                                       String description, String status, Instant occurredAt) {
    public static EmployeeTransferResponse from(FinancialOperation operation) {
        var source = operation.getSourceAccount();
        var target = operation.getTargetAccount();
        return new EmployeeTransferResponse(operation.getId(), operation.getReference(),
                source == null ? "—" : source.getOwner().getFirstName() + " " + source.getOwner().getLastName(),
                target == null ? "—" : target.getOwner().getFirstName() + " " + target.getOwner().getLastName(),
                source == null ? "—" : "•••• " + source.getAccountNumber().substring(Math.max(0, source.getAccountNumber().length() - 4)),
                target == null ? "—" : "•••• " + target.getAccountNumber().substring(Math.max(0, target.getAccountNumber().length() - 4)),
                operation.getAmount(), operation.getCurrency(), operation.getDescription(), operation.getStatus(), operation.getOccurredAt());
    }
}

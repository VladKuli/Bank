package com.bank.bank_system.web.client;

import com.bank.bank_system.domain.FinancialOperation;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record CustomerTransactionResponse(UUID id, String reference, String type, BigDecimal amount,
        String currency, Instant occurredAt, String description, String status, boolean incoming,
        String otherParty, String accountLastFour, UUID accountId) {
    public static CustomerTransactionResponse from(FinancialOperation operation, UUID customerId) {
        boolean incoming = operation.getTargetAccount() != null && operation.getTargetAccount().getOwner().getId().equals(customerId);
        String otherParty = null;
        if (operation.getType().name().equals("TRANSFER")) {
            var otherAccount = incoming ? operation.getSourceAccount() : operation.getTargetAccount();
            if (otherAccount != null) otherParty = otherAccount.getOwner().getFirstName() + " " + otherAccount.getOwner().getLastName();
        }
        var account = incoming ? operation.getTargetAccount() : operation.getSourceAccount();
        String lastFour = account == null ? null : "•••• " + account.getAccountNumber().substring(Math.max(0, account.getAccountNumber().length() - 4));
        return new CustomerTransactionResponse(operation.getId(), operation.getReference(), operation.getType().name(),
                operation.getAmount(), operation.getCurrency(), operation.getOccurredAt(), operation.getDescription(),
                operation.getStatus(), incoming, otherParty, lastFour, account == null ? null : account.getId());
    }
}

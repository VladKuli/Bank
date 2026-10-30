package com.bank.bank_system.domain;

import com.bank.bank_system.domain.enums.FinancialOperationType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;
import java.util.Locale;

@Entity
@Table(name = "financial_operations")
@Getter
public class FinancialOperation {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private FinancialOperationType type;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "source_account_id")
    private Account sourceAccount;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "target_account_id")
    private Account targetAccount;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal amount;

    @Column(nullable = false, length = 3)
    private String currency;

    @Column(name = "transaction_reference", nullable = false, unique = true, length = 48)
    private String reference;

    @Column(columnDefinition = "text")
    private String description;

    @Column(nullable = false, length = 16)
    private String status = "COMPLETED";

    @Column(name = "idempotency_key", unique = true)
    private UUID idempotencyKey;

    @Column(name = "occurred_at", nullable = false, updatable = false)
    private Instant occurredAt;

    protected FinancialOperation() {
    }

    public FinancialOperation(FinancialOperationType type, Account sourceAccount,
                              Account targetAccount, BigDecimal amount, String currency) {
        this(type, sourceAccount, targetAccount, amount, currency, null);
    }

    public FinancialOperation(FinancialOperationType type, Account sourceAccount,
                              Account targetAccount, BigDecimal amount, String currency, String description) {
        this(type, sourceAccount, targetAccount, amount, currency, description, null);
    }

    public FinancialOperation(FinancialOperationType type, Account sourceAccount,
                              Account targetAccount, BigDecimal amount, String currency, String description, UUID idempotencyKey) {
        this.type = type;
        this.sourceAccount = sourceAccount;
        this.targetAccount = targetAccount;
        this.amount = amount;
        this.currency = currency;
        this.description = description;
        this.idempotencyKey = idempotencyKey;
        this.reference = "NB-" + UUID.randomUUID().toString().replace("-", "").toUpperCase(Locale.ROOT);
    }

    @PrePersist
    void setOccurredAt() {
        if (occurredAt == null) {
            occurredAt = Instant.now();
        }
    }
}

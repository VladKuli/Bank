package com.bank.bank_system.domain;

import com.bank.bank_system.domain.enums.AccountStatus;
import com.bank.bank_system.domain.enums.AccountType;
import com.bank.bank_system.domain.enums.ClientStatus;
import com.bank.bank_system.exception.AccountBalanceMustBeZeroException;
import com.bank.bank_system.exception.AccountNotOpenException;
import com.bank.bank_system.exception.BlockedClientOperationException;
import com.bank.bank_system.exception.InsufficientFundsException;
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

@Entity
@Table(name = "accounts")
@Getter
public class Account {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "account_number", nullable = false, unique = true, length = 16)
    private String accountNumber;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "client_id", nullable = false)
    private Client owner;

    @Enumerated(EnumType.STRING)
    @Column(name = "account_type", nullable = false, length = 16)
    private AccountType type;

    @Column(nullable = false, length = 3)
    private String currency;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal balance = BigDecimal.ZERO.setScale(2);

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private AccountStatus status = AccountStatus.OPEN;

    @Column(name = "opened_at", nullable = false, updatable = false)
    private Instant openedAt;

    protected Account() {
    }

    public Account(String accountNumber, Client owner, AccountType type, String currency) {
        this.accountNumber = accountNumber;
        this.owner = owner;
        this.type = type;
        this.currency = currency;
        this.balance = BigDecimal.ZERO.setScale(2);
        this.status = AccountStatus.OPEN;
    }

    public void credit(BigDecimal amount) {
        balance = balance.add(amount);
    }

    public void debit(BigDecimal amount) {
        if (balance.compareTo(amount) < 0) {
            throw new InsufficientFundsException();
        }
        balance = balance.subtract(amount);
    }

    public void close() {
        if (balance.signum() != 0) {
            throw new AccountBalanceMustBeZeroException();
        }
        status = AccountStatus.CLOSED;
    }

    public void freeze() {
        if (status != AccountStatus.OPEN) {
            throw new IllegalStateException("Only open accounts can be frozen");
        }
        status = AccountStatus.FROZEN;
    }

    public AccountStatus status() {
        return status;
    }

    public UUID id() {
        return id;
    }

    public void unfreeze() {
        if (status != AccountStatus.FROZEN) {
            throw new IllegalStateException("Only frozen accounts can be unfrozen");
        }
        status = AccountStatus.OPEN;
    }

    public void ensureOpen() {
        if (status != AccountStatus.OPEN) {
            throw new AccountNotOpenException(accountNumber);
        }
        if (owner.getStatus() == ClientStatus.BLOCKED) {
            throw new BlockedClientOperationException();
        }
    }

    @PrePersist
    void setOpenedAt() {
        if (openedAt == null) {
            openedAt = Instant.now();
        }
    }

}

package com.bank.bank_system.service.impl;

import com.bank.bank_system.domain.Account;
import com.bank.bank_system.exception.AccountNotFoundException;
import com.bank.bank_system.domain.enums.AccountType;
import com.bank.bank_system.domain.FinancialOperation;
import com.bank.bank_system.domain.enums.FinancialOperationType;
import com.bank.bank_system.exception.InvalidAmountException;
import com.bank.bank_system.exception.SameAccountTransferException;
import com.bank.bank_system.exception.CurrencyMismatchException;
import com.bank.bank_system.infrastructure.AccountRepository;
import com.bank.bank_system.infrastructure.FinancialOperationRepository;
import com.bank.bank_system.domain.Client;
import com.bank.bank_system.exception.ClientNotFoundException;
import com.bank.bank_system.infrastructure.ClientRepository;
import java.math.BigDecimal;
import java.security.SecureRandom;
import java.util.List;
import java.util.UUID;

import com.bank.bank_system.service.AccountService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AccountServiceImpl implements AccountService {

    private static final long MIN_ACCOUNT_NUMBER = 1_000_000_000_000_000L;
    private static final long MAX_ACCOUNT_NUMBER = 10_000_000_000_000_000L;
    private static final SecureRandom RANDOM = new SecureRandom();

    private final AccountRepository accountRepository;
    private final FinancialOperationRepository operationRepository;
    private final ClientRepository clientRepository;

    @Transactional
    public Account open(UUID clientId, AccountType type, String currency) {
        Client client = clientRepository.findById(clientId)
                .orElseThrow(() -> new ClientNotFoundException(clientId));
        String normalizedCurrency = currency.toUpperCase(java.util.Locale.ROOT);
        return accountRepository.save(new Account(generateAccountNumber(), client, type, normalizedCurrency));
    }

    @Transactional(readOnly = true)
    public List<Account> listForClient(UUID clientId) {
        if (!clientRepository.existsById(clientId)) {
            throw new ClientNotFoundException(clientId);
        }
        return accountRepository.findByOwnerIdOrderByOpenedAtDesc(clientId);
    }

    @Transactional(readOnly = true)
    public Account get(UUID id) {
        return accountRepository.findByIdWithOwner(id).orElseThrow(() -> new AccountNotFoundException(id));
    }

    @Transactional
    public Account close(UUID id) {
        Account account = lock(id);
        account.ensureOpen();
        account.close();
        return account;
    }

    @Transactional
    public FinancialOperation deposit(UUID accountId, BigDecimal amount) {
        BigDecimal normalizedAmount = validateAmount(amount);
        Account account = lock(accountId);
        account.ensureOpen();
        account.credit(normalizedAmount);
        return operationRepository.save(new FinancialOperation(
                FinancialOperationType.DEPOSIT, null, account, normalizedAmount, account.getCurrency()));
    }

    @Transactional
    public FinancialOperation withdraw(UUID accountId, BigDecimal amount) {
        BigDecimal normalizedAmount = validateAmount(amount);
        Account account = lock(accountId);
        account.ensureOpen();
        account.debit(normalizedAmount);
        return operationRepository.save(new FinancialOperation(
                FinancialOperationType.WITHDRAWAL, account, null, normalizedAmount, account.getCurrency()));
    }

    @Transactional
    public FinancialOperation transfer(UUID sourceId, UUID targetId, BigDecimal amount) {
        return transfer(sourceId, targetId, amount, null);
    }

    @Transactional
    public FinancialOperation transfer(UUID sourceId, UUID targetId, BigDecimal amount, String description) {
        return transfer(sourceId, targetId, amount, description, null);
    }

    @Transactional
    public FinancialOperation transfer(UUID sourceId, UUID targetId, BigDecimal amount, String description, UUID idempotencyKey) {
        if (sourceId.equals(targetId)) {
            throw new SameAccountTransferException();
        }
        BigDecimal normalizedAmount = validateAmount(amount);

        // Lock in a stable order to reduce deadlocks when transfers happen in opposite directions.
        Account first = sourceId.compareTo(targetId) < 0 ? lock(sourceId) : lock(targetId);
        Account second = sourceId.compareTo(targetId) < 0 ? lock(targetId) : lock(sourceId);
        Account source = first.getId().equals(sourceId) ? first : second;
        Account target = first.getId().equals(targetId) ? first : second;

        source.ensureOpen();
        target.ensureOpen();
        if (!source.getCurrency().equals(target.getCurrency())) {
            throw new CurrencyMismatchException();
        }

        source.debit(normalizedAmount);
        target.credit(normalizedAmount);
        return operationRepository.save(new FinancialOperation(
                FinancialOperationType.TRANSFER, source, target, normalizedAmount, source.getCurrency(), description, idempotencyKey));
    }

    @Transactional(readOnly = true)
    public List<FinancialOperation> history(UUID accountId) {
        if (!accountRepository.existsById(accountId)) {
            throw new AccountNotFoundException(accountId);
        }
        return operationRepository.findHistory(accountId);
    }

    private Account lock(UUID id) {
        return accountRepository.findByIdForUpdate(id).orElseThrow(() -> new AccountNotFoundException(id));
    }

    private BigDecimal validateAmount(BigDecimal amount) {
        if (amount == null || amount.signum() <= 0 || amount.stripTrailingZeros().scale() > 2) {
            throw new InvalidAmountException();
        }
        return amount.setScale(2);
    }

    private String generateAccountNumber() {
        String accountNumber;
        do {
            accountNumber = Long.toString(RANDOM.nextLong(MIN_ACCOUNT_NUMBER, MAX_ACCOUNT_NUMBER));
        } while (accountRepository.existsByAccountNumber(accountNumber));
        return accountNumber;
    }
}

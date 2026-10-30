package com.bank.bank_system.service.impl;

import com.bank.bank_system.domain.Account;
import com.bank.bank_system.domain.enums.AccountStatus;
import com.bank.bank_system.exception.AccountNotFoundException;
import com.bank.bank_system.infrastructure.AccountRepository;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
@RequiredArgsConstructor
public class CustomerAccountStatusService {
    private final AccountRepository accounts;

    @Transactional
    public Account updateStatus(UUID ownerId, UUID accountId, AccountStatus requestedStatus) {
        Account account = accounts.findByIdForUpdate(accountId)
                .orElseThrow(() -> new AccountNotFoundException(accountId));
        if (!accounts.existsByIdAndOwnerId(accountId, ownerId)) {
            throw new AccountNotFoundException(accountId);
        }
        if (account.status() == requestedStatus) return account;
        if (account.status() == AccountStatus.OPEN && requestedStatus == AccountStatus.FROZEN) {
            account.freeze();
        } else if (account.status() == AccountStatus.FROZEN && requestedStatus == AccountStatus.OPEN) {
            account.unfreeze();
        } else {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Only open accounts can be frozen, and only frozen accounts can be unfrozen");
        }
        return account;
    }
}

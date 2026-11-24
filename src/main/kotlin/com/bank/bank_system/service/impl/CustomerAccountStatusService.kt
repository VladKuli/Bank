package com.bank.bank_system.service.impl

import com.bank.bank_system.domain.Account
import com.bank.bank_system.domain.enums.AccountStatus
import com.bank.bank_system.exception.AccountNotFoundException
import com.bank.bank_system.infrastructure.AccountRepository
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.server.ResponseStatusException
import java.util.UUID

@Service
class CustomerAccountStatusService(
    private val accounts: AccountRepository,
) {
    @Transactional
    fun updateStatus(ownerId: UUID, accountId: UUID, requestedStatus: AccountStatus): Account {
        val account = accounts.findByIdForUpdate(accountId)
            .orElseThrow { AccountNotFoundException(accountId) }

        if (!accounts.existsByIdAndOwnerId(accountId, ownerId)) {
            throw AccountNotFoundException(accountId)
        }

        when {
            account.status() == requestedStatus -> return account
            account.status() == AccountStatus.OPEN && requestedStatus == AccountStatus.FROZEN -> account.freeze()
            account.status() == AccountStatus.FROZEN && requestedStatus == AccountStatus.OPEN -> account.unfreeze()
            else -> throw ResponseStatusException(
                HttpStatus.CONFLICT,
                "Only open accounts can be frozen, and only frozen accounts can be unfrozen",
            )
        }

        return account
    }
}

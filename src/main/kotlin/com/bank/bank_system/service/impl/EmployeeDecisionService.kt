package com.bank.bank_system.service.impl

import com.bank.bank_system.domain.PaymentCard
import com.bank.bank_system.domain.enums.AccountStatus
import com.bank.bank_system.domain.enums.MembershipTier
import com.bank.bank_system.infrastructure.AccountRepository
import com.bank.bank_system.infrastructure.CardRequestRepository
import com.bank.bank_system.infrastructure.ClientRepository
import com.bank.bank_system.infrastructure.LoanApplicationRepository
import com.bank.bank_system.infrastructure.MembershipRequestRepository
import com.bank.bank_system.infrastructure.PaymentCardRepository
import com.bank.bank_system.web.client.CustomerViews
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.server.ResponseStatusException
import java.security.SecureRandom
import java.util.Locale
import java.util.UUID

@Service
class EmployeeDecisionService(
    private val cardRequests: CardRequestRepository,
    private val membershipRequests: MembershipRequestRepository,
    private val loanApplications: LoanApplicationRepository,
    private val cards: PaymentCardRepository,
    private val accounts: AccountRepository,
    private val clients: ClientRepository,
) {
    @Transactional
    fun decideCardRequest(id: UUID, status: String, reason: String?, employee: String): CustomerViews.Request {
        val normalizedReason = validateDecision(status, reason)
        val request = cardRequests.findByIdForUpdate(id)
            .orElseThrow { ResponseStatusException(HttpStatus.NOT_FOUND, "Pending card request not found") }
        requirePending(request.status(), "card request")

        if (status == APPROVED) {
            val owner = request.ownerForDecision()
            val accountId = request.accountForDecision().id()
            val account = accounts.findByIdForUpdate(accountId)
                .orElseThrow { ResponseStatusException(HttpStatus.NOT_FOUND, "Linked account not found") }
            if (account.status() != AccountStatus.OPEN) {
                throw ResponseStatusException(HttpStatus.CONFLICT, "The linked account must be open before issuing a card")
            }
            if (cards.existsByOwnerIdAndAccountIdAndTierAndStatusNot(
                    owner.id(), account.id(), request.tierForDecision(), BLOCKED,
                )
            ) {
                throw ResponseStatusException(HttpStatus.CONFLICT, "A card of this tier is already active for the account")
            }
            cards.save(PaymentCard(owner, account, request.tierForDecision(), lastFour()))
        }

        request.decide(status, normalizedReason, employee)
        return CustomerViews.Request.from(request)
    }

    @Transactional
    fun decideMembershipRequest(id: UUID, status: String, reason: String?, employee: String): CustomerViews.Membership {
        val normalizedReason = validateDecision(status, reason)
        val request = membershipRequests.findByIdForUpdate(id)
            .orElseThrow { ResponseStatusException(HttpStatus.NOT_FOUND, "Pending membership request not found") }
        requirePending(request.status(), "membership request")

        if (status == APPROVED) {
            val ownerId = request.ownerForDecision().id()
            val owner = clients.findByIdForUpdate(ownerId)
                .orElseThrow { ResponseStatusException(HttpStatus.NOT_FOUND, "Customer not found") }
            if (rank(request.requestedTierForDecision()) <= rank(owner.membershipTier())) {
                throw ResponseStatusException(HttpStatus.CONFLICT, "The requested membership is no longer an upgrade")
            }
            owner.setMembershipTier(request.requestedTierForDecision())
        }

        request.decide(status, normalizedReason, employee)
        return CustomerViews.Membership.from(request)
    }

    @Transactional
    fun decideLoan(id: UUID, status: String, reason: String?, employee: String): CustomerViews.Loan {
        val normalizedReason = validateDecision(status, reason)
        val application = loanApplications.findByIdForUpdate(id)
            .orElseThrow { ResponseStatusException(HttpStatus.NOT_FOUND, "Pending loan application not found") }
        requirePending(application.status(), "loan application")

        application.decide(status, normalizedReason, employee)
        return CustomerViews.Loan.from(application)
    }

    private fun validateDecision(status: String, reason: String?): String? {
        if (status !in decisionStatuses) {
            throw ResponseStatusException(HttpStatus.BAD_REQUEST, "Decision status must be APPROVED or REJECTED")
        }

        val normalizedReason = reason?.trim()?.takeIf(String::isNotEmpty)
        if (status == REJECTED && normalizedReason == null) {
            throw ResponseStatusException(HttpStatus.BAD_REQUEST, "A reason is required for a rejection")
        }
        return normalizedReason
    }

    private fun requirePending(currentStatus: String, requestType: String) {
        if (currentStatus != PENDING) {
            throw ResponseStatusException(HttpStatus.NOT_FOUND, "Pending $requestType not found")
        }
    }

    private fun rank(tier: MembershipTier): Int = when (tier) {
        MembershipTier.STANDARD -> 0
        MembershipTier.PREMIUM -> 1
        MembershipTier.VIP -> 2
    }

    private fun lastFour(): String = String.format(Locale.ROOT, "%04d", secureRandom.nextInt(10_000))

    private companion object {
        const val APPROVED = "APPROVED"
        const val REJECTED = "REJECTED"
        const val PENDING = "PENDING"
        const val BLOCKED = "BLOCKED"
        val decisionStatuses = setOf(APPROVED, REJECTED)
        val secureRandom = SecureRandom()
    }
}

package com.bank.bank_system.service.impl

import com.bank.bank_system.infrastructure.PaymentCardRepository
import com.bank.bank_system.web.client.CustomerViews
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.server.ResponseStatusException
import java.util.UUID

@Service
class CustomerCardStatusService(
    private val cards: PaymentCardRepository,
) {
    @Transactional
    fun updateStatus(ownerId: UUID, cardId: UUID, requestedStatus: String): CustomerViews.Card {
        if (requestedStatus !in allowedStatuses) {
            throw ResponseStatusException(HttpStatus.BAD_REQUEST, "Unsupported card status")
        }

        val card = cards.findByIdForUpdate(cardId)
            .orElseThrow { ResponseStatusException(HttpStatus.NOT_FOUND, "Card not found") }

        if (!cards.existsByIdAndOwnerId(cardId, ownerId)) {
            throw ResponseStatusException(HttpStatus.NOT_FOUND, "Card not found")
        }

        val currentStatus = card.status()
        if (currentStatus == requestedStatus) return CustomerViews.Card.from(card)

        val canTransition = when (currentStatus) {
            "ACTIVE" -> requestedStatus == "FROZEN" || requestedStatus == "BLOCKED"
            "FROZEN" -> requestedStatus == "ACTIVE" || requestedStatus == "BLOCKED"
            "BLOCKED" -> false
            else -> false
        }

        if (!canTransition) {
            throw ResponseStatusException(
                HttpStatus.CONFLICT,
                "A blocked card cannot be reactivated",
            )
        }

        card.setStatus(requestedStatus)
        return CustomerViews.Card.from(card)
    }

    private companion object {
        val allowedStatuses = setOf("ACTIVE", "FROZEN", "BLOCKED")
    }
}

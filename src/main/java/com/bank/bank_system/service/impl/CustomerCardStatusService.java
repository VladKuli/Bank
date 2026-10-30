package com.bank.bank_system.service.impl;

import com.bank.bank_system.domain.PaymentCard;
import com.bank.bank_system.infrastructure.PaymentCardRepository;
import com.bank.bank_system.web.client.CustomerViews;
import java.util.Set;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
@RequiredArgsConstructor
public class CustomerCardStatusService {
    private static final Set<String> ALLOWED_STATUSES = Set.of("ACTIVE", "FROZEN", "BLOCKED");
    private final PaymentCardRepository cards;

    @Transactional
    public CustomerViews.Card updateStatus(UUID ownerId, UUID cardId, String requestedStatus) {
        if (!ALLOWED_STATUSES.contains(requestedStatus)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Unsupported card status");
        }
        PaymentCard card = cards.findByIdForUpdate(cardId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Card not found"));
        if (!cards.existsByIdAndOwnerId(cardId, ownerId)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Card not found");
        }
        String current = card.status();
        if (current.equals(requestedStatus)) return CustomerViews.Card.from(card);
        boolean allowed = switch (current) {
            case "ACTIVE" -> requestedStatus.equals("FROZEN") || requestedStatus.equals("BLOCKED");
            case "FROZEN" -> requestedStatus.equals("ACTIVE") || requestedStatus.equals("BLOCKED");
            default -> false;
        };
        if (!allowed) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "A blocked card cannot be reactivated");
        }
        card.setStatus(requestedStatus);
        return CustomerViews.Card.from(card);
    }
}

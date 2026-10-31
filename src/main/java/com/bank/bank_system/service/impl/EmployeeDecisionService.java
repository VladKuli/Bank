package com.bank.bank_system.service.impl;

import com.bank.bank_system.domain.Account;
import com.bank.bank_system.domain.CardRequest;
import com.bank.bank_system.domain.Client;
import com.bank.bank_system.domain.LoanApplication;
import com.bank.bank_system.domain.MembershipRequest;
import com.bank.bank_system.domain.PaymentCard;
import com.bank.bank_system.domain.enums.AccountStatus;
import com.bank.bank_system.domain.enums.MembershipTier;
import com.bank.bank_system.infrastructure.AccountRepository;
import com.bank.bank_system.infrastructure.CardRequestRepository;
import com.bank.bank_system.infrastructure.ClientRepository;
import com.bank.bank_system.infrastructure.LoanApplicationRepository;
import com.bank.bank_system.infrastructure.MembershipRequestRepository;
import com.bank.bank_system.infrastructure.PaymentCardRepository;
import com.bank.bank_system.web.client.CustomerViews;
import java.security.SecureRandom;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
@RequiredArgsConstructor
public class EmployeeDecisionService {
    private static final String APPROVED = "APPROVED";
    private static final String REJECTED = "REJECTED";
    private static final String PENDING = "PENDING";
    private static final String BLOCKED = "BLOCKED";
    private static final Set<String> DECISION_STATUSES = Set.of(APPROVED, REJECTED);
    private static final SecureRandom RANDOM = new SecureRandom();

    private final CardRequestRepository cardRequests;
    private final MembershipRequestRepository membershipRequests;
    private final LoanApplicationRepository loanApplications;
    private final PaymentCardRepository cards;
    private final AccountRepository accounts;
    private final ClientRepository clients;

    @Transactional
    public CustomerViews.Request decideCardRequest(UUID id, String status, String reason, String employee) {
        String normalizedReason = validateDecision(status, reason);
        CardRequest request = cardRequests.findByIdForUpdate(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Pending card request not found"));
        requirePending(request.status(), "card request");
        if (APPROVED.equals(status)) {
            Client owner = request.ownerForDecision();
            Account account = accounts.findByIdForUpdate(request.accountForDecision().id())
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Linked account not found"));
            if (account.status() != AccountStatus.OPEN) {
                throw new ResponseStatusException(HttpStatus.CONFLICT, "The linked account must be open before issuing a card");
            }
            if (cards.existsByOwnerIdAndAccountIdAndTierAndStatusNot(owner.id(), account.id(), request.tierForDecision(), BLOCKED)) {
                throw new ResponseStatusException(HttpStatus.CONFLICT, "A card of this tier is already active for the account");
            }
            cards.save(new PaymentCard(owner, account, request.tierForDecision(), lastFour()));
        }
        request.decide(status, normalizedReason, employee);
        return CustomerViews.Request.from(request);
    }

    @Transactional
    public CustomerViews.Membership decideMembershipRequest(UUID id, String status, String reason, String employee) {
        String normalizedReason = validateDecision(status, reason);
        MembershipRequest request = membershipRequests.findByIdForUpdate(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Pending membership request not found"));
        requirePending(request.status(), "membership request");
        if (APPROVED.equals(status)) {
            UUID ownerId = request.ownerForDecision().id();
            Client owner = clients.findByIdForUpdate(ownerId)
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Customer not found"));
            if (rank(request.requestedTierForDecision()) <= rank(owner.membershipTier())) {
                throw new ResponseStatusException(HttpStatus.CONFLICT, "The requested membership is no longer an upgrade");
            }
            owner.setMembershipTier(request.requestedTierForDecision());
        }
        request.decide(status, normalizedReason, employee);
        return CustomerViews.Membership.from(request);
    }

    @Transactional
    public CustomerViews.Loan decideLoan(UUID id, String status, String reason, String employee) {
        String normalizedReason = validateDecision(status, reason);
        LoanApplication application = loanApplications.findByIdForUpdate(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Pending loan application not found"));
        requirePending(application.status(), "loan application");
        application.decide(status, normalizedReason, employee);
        return CustomerViews.Loan.from(application);
    }

    private String validateDecision(String status, String reason) {
        if (!DECISION_STATUSES.contains(status)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Decision status must be APPROVED or REJECTED");
        }
        String normalized = reason == null || reason.isBlank() ? null : reason.trim();
        if (REJECTED.equals(status) && normalized == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "A reason is required for a rejection");
        }
        return normalized;
    }

    private void requirePending(String current, String type) {
        if (!PENDING.equals(current)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Pending " + type + " not found");
        }
    }

    private int rank(MembershipTier tier) {
        return switch (tier) {
            case STANDARD -> 0;
            case PREMIUM -> 1;
            case VIP -> 2;
        };
    }

    private String lastFour() {
        return String.format(Locale.ROOT, "%04d", RANDOM.nextInt(10_000));
    }
}
